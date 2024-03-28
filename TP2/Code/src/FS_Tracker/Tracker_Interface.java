package FS_Tracker;

import Files.FileInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Tracker_Interface implements Runnable
{
    FS_Tracker tracker;


    public Tracker_Interface(FS_Tracker tracker)
    {
        this.tracker = tracker;
    }

    public void header() throws UnknownHostException {
        clear();
        System.out.println("FS Tracker");
        System.out.println("Server active at " + InetAddress.getLocalHost().getHostName() + " (IP: " + InetAddress.getLocalHost().getHostAddress() + ") in port " + this.tracker.getPort());
        System.out.println("--------------------------------------------------");
    }

    public static void clear()
    {
        System.out.print("\033\143");
        System.out.flush();
    }

    public void mainMenu() throws UnknownHostException {
        clear();
        this.header();
        System.out.println();
        System.out.println("Main Menu");
        System.out.println();
        System.out.println("1 - See list of connected Nodes");
        System.out.println("2 - See all shared files");
        System.out.println("3 - See Nodes that contain a specific file");
        System.out.println("4 - See logs");
        System.out.println("5 - Close Program");
        System.out.println();
        System.out.println("Insert option bellow:");
    }

    public void listConnectedNodes(List<String> nodes) throws UnknownHostException {
        int i = 1;
        clear();
        this.header();
        System.out.println();
        System.out.println("Connected Nodes in the network:");
        System.out.println();
        for (String node : nodes)
        {
            System.out.println(i + " - " + node);
            i++;
        }
        System.out.println();
        System.out.println("Press ' 0 ' to go back");
    }

    public void listAllFiles(ArrayList<FileInfo> files) throws UnknownHostException {
        int i = 1;
        this.header();
        System.out.println();
        System.out.println("All shared files: ");
        System.out.println();
        for (FileInfo file : files)
        {
            System.out.println(i + " - File Name: " + file.getFileName() + "; Size: " + file.getFileSize());
            i++;
        }
        System.out.println();
        System.out.println("Press ' 0 ' to go back");
    }

    public void nodesWithFile(List<String> nodes, String filename) throws UnknownHostException {
        int i = 1;
        header();
        System.out.println();
        System.out.println("Nodes with file '" + filename + "'");
        System.out.println();
        for (String node : nodes)
        {
            System.out.println(i + " - " + node);
            i++;
        }
        System.out.println();
        System.out.println("Press ' 0 ' to go back");
    }

    public void showLogs(List<String> logs) throws UnknownHostException {
        header();
        System.out.println();
        System.out.println("Logs:");
        System.out.println();
        for (String str: logs)
        {
            System.out.println(str);
        }
        System.out.println();
        System.out.println("Press ' 0 ' to go back");
    }

    public void Navigator() throws UnknownHostException {
        int option = 0;
        Scanner scanner = new Scanner(System.in);

        do {
            switch (option)
            {
                case 0:
                    mainMenu();
                    option = scanner.nextInt();

                    if (option < 1 || option > 5)
                    {
                        option = 0;
                    }
                    break;
                case 1:
                    listConnectedNodes(this.tracker.getConnectedNodes());
                    option = scanner.nextInt();
                    if (option != 0)
                    {
                        option = 1;
                    }
                    break;
                case 2:
                    listAllFiles(this.tracker.getAllFileList(""));
                    option = scanner.nextInt();
                    if (option != 0)
                    {
                        option = 2;
                    }
                    break;
                case 3: // TODO THIS OPTION
                    header();
                    System.out.println("Insert file name bellow:");
                    System.out.println();
                    String filename = scanner.next();
                    List<String> nodes = this.tracker.getNodesWithFile(filename);
                    if (nodes.isEmpty())
                    {
                        header();
                        System.out.println("ERROR: File '" +  filename +  "' not found!");
                        System.out.println("Press ' 0 ' to go back");
                    }
                    else
                    {
                        nodesWithFile(nodes,filename);
                    }
                    option = scanner.nextInt();
                    if (option != 0)
                    {
                        option = 3;
                    } else option = 0;
                    break;
                case 4:
                    showLogs(this.tracker.getLogs());
                    option = scanner.nextInt();
                    option = 0;
                    break;
                case 5:
                    option = 10;
                    System.exit(0); // TODO Close manager and connections.
                    break;
            }
        } while (option != 10);
    }


    @Override
    public void run()
    {
        try {
            Navigator();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
