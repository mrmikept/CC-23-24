package FS_Tracker;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class TrackerConnection_Manager implements Runnable
{
    FS_Tracker tracker;
    ServerSocket socket;
    boolean isActive;

    public TrackerConnection_Manager(FS_Tracker tracker, int port) throws IOException {
        try {
            this.tracker = tracker;
            this.socket = new ServerSocket(port);
            this.tracker.setServerActive(true);
            this.tracker.setPort(port);
            this.isActive = true;
        } catch (IOException e)
        {
            System.out.println("ERROR: " + e.getMessage() + "\n Couldn't connect to socket.\n Closing Tracker....");
            System.exit(1);
        }
    }

    @Override
    public void run()
    {
        ArrayList<Thread> threads = new ArrayList<>();
        try
        {
            try {
                this.tracker.addLog("FS_Tacker.FS_Tracker server active at " + InetAddress.getLocalHost().getHostName() + " (IP: " + InetAddress.getLocalHost().getHostAddress() + ") in port " + this.socket.getLocalPort());
                while (this.isActive)
                {
                    Socket client = null;
                    try {
                        client = this.socket.accept();
                        this.tracker.addLog("New node connected: " + InetAddress.getByAddress(client.getInetAddress().getAddress()).getHostName());
                        this.tracker.addNode(InetAddress.getByAddress(client.getInetAddress().getAddress()).getHostName());
                        DataInputStream inputStream = new DataInputStream(client.getInputStream());
                        DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
                        this.tracker.addLog("Creating a new thread for this node...");

                        Thread thread = new Thread(new TrackerConnection_Handler(this.tracker, InetAddress.getByAddress(client.getInetAddress().getAddress()).getHostName(), outputStream, inputStream, client));
                        threads.add(thread);
                        this.tracker.addLog("Thread created, starting thread");
                        thread.start();
                    } catch (Exception e) {
                        assert client != null;
                        client.close();
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            for (Thread t : threads)
            {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                this.socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
