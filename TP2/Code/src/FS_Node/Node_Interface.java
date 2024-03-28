package FS_Node;

import Files.FileInfo;
import DataUtilities.Serializer;
import FS_Tracker_Protocol.FS_Tracker_Protocol;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Node_Interface implements Runnable
{
    private FS_Node node;

    public Node_Interface(FS_Node node)
    {
        this.node = node;
    }

    public static void clear()
    {
        System.out.print("\033\143");
        System.out.flush();
    }

    public void header()
    {
        System.out.println("FS Node");
        if (this.node.isOnline())
        {
            System.out.println("Connected with FS_Tracker at " + this.node.getTrackerAddr() + " port " + this.node.getPort());
            System.out.println("FS_Transfer_Protocol: Listening at UDP port " + this.node.getPort());
        }
        System.out.println("--------------------------------------------------");
    }

    public void mainMenu()
    {
        clear();
        this.header();
        System.out.println();
        System.out.println("Main Menu");
        System.out.println();
        System.out.println("1 - Transfer File");
        System.out.println("2 - See my shared folder content");
        System.out.println("3 - See file transfer status");
        System.out.println("4 - Show logs");
        System.out.println("5 - Close Program");
        System.out.println();
        System.out.println("Insert option bellow:");
    }

    public void listFiles(List<FileInfo> fileList)
    {
        int i = 1;
        for (FileInfo file : fileList)
        {
            System.out.println(i + " - " + file.getFileName() + "; Size: " + file.getFileSize());
            i++;
        }
    }

    public void listFilesSharedFolder(List<FileInfo> fileList)
    {
        clear();
        header();
        System.out.println();
        listFiles(fileList);
        System.out.println();
        System.out.println("Press ' 0 ' to go back");
    }

    public int listFilesToTransfer(List<FileInfo> fileList)
    {
        if (fileList != null)
        {
            clear();
            header();
            System.out.println();
            listFiles(fileList);
            System.out.println();
            System.out.println("Choose one of the following actions:");
            System.out.println("1 - Transfer file");
            System.out.println("0 - Go back");
            System.out.println();
            int option = new Scanner(System.in).nextInt();
            if (option < 0 || option > 1)
            {
                return this.listFilesToTransfer(null);
            }
            else return option;
        } else return errorPage("No files found to transfer");
    }

    public int listTransferStatus(List<String> transfers)
    {
        if (!transfers.isEmpty())
        {
            clear();
            header();
            System.out.println();
            int i = 1;
            for (String string : transfers)
            {
                System.out.println(i + " - "  + string);
                i++;
            }
            System.out.println();
            System.out.println("Press ' 0 ' to go back");
            int option = new Scanner(System.in).nextInt();
            if (option != 0)
            {
                return this.listTransferStatus(transfers);
            }
            else return option;
        } else return this.errorPage("Didn't found new file transfers");
    }

    public void showLogs(List<String> logList)
    {
        clear();
        header();
        System.out.println();
        System.out.println("Logs:");
        System.out.println();
        for (String str: logList)
        {
            System.out.println(str);
        }
        System.out.println();
        System.out.println("Press ' 0 ' to go back");
    }

    public int errorPage(String message)
    {
        clear();
        header();
        System.out.println();
        System.out.println("ERROR: " + message);
        System.out.println();
        System.out.println("Press ' 0 ' to go back");
        int option = new Scanner(System.in).nextInt();
        if (option != 0)
        {
            return errorPage(message);
        } else return option;
    }

    public void navigator() throws IOException, InterruptedException {
        int option = 0;
        Scanner input = new Scanner(System.in);

        do {
            switch (option)
            {
                case 0: // Main Menu
                    mainMenu();
                    option = input.nextInt();
                    if (option < 1 || option > 5)
                    {
                        option = 0;
                    }
                    break;
                case 1: // List Available files
                    option = listFilesToTransfer(Serializer.byteArrayToArrayListFileInfo(this.node.sendMessageAndWaitResponse(FS_Tracker_Protocol.FILE_LIST_REQUEST,"Send me some files pleasseee :)".getBytes())));
                    if (option < 0 || option > 1)
                    {
                        option = 1;
                    }
                    if (option == 1) // Transfer a file option
                    {
                        System.out.println();
                        System.out.println("Enter the file name to start transfer or ' exit ' to go back:");
                        String filename = input.next();
                        if (filename.equals("exit"))
                        {
                            option = 1;
                            break;
                        }
                        else
                        {
                            Map<String,FileInfo> map = Serializer.serializableByteArrayToMapNodeFileInfo(this.node.sendMessageAndWaitResponse(FS_Tracker_Protocol.FILE_LOCATION_REQUEST,filename.getBytes()));
                            if (map != null)
                            {
                                this.node.transferFile(map);
                                clear();
                                header();
                                System.out.println();
                                System.out.println("File transfer started!");
                                System.out.println();
                                System.out.println("Press any key to go back");
                                input.nextLine();
                                option = 0;
                                break;
                            }
                            else
                            {
                                option = this.errorPage("Something went wrong, sorry.");
                            }
                        }
                    }
                    break;
                case 2: // See shared folder
                    listFilesSharedFolder(this.node.getFileList());
                    option = input.nextInt();
                    if (option != 0)
                    {
                        option = 2;
                    }
                    break;
                case 3:
                    option = listTransferStatus(this.node.getFileTransfers());
                    if (option != 0)
                    {
                        option = 3;
                    }
                    break;
                case 4: // Show Logs
                    showLogs(this.node.getLogs());
                    option = input.nextInt();
                    if (option != 0)
                    {
                        option = 1;
                    }
                    break;
                case 5: // Close program
                    option = 10;
//                    this.node.stopProgram();
                    break;
            }
        } while (option != 10);
    }


    @Override
    public void run() {
        try {
            navigator();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
