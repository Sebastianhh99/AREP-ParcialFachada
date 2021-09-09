package co.escuelaing.arep.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HttpServer {
    private static HttpServer _instance = new HttpServer();

    private static HashMap<String,String> paths;

    private HttpServer(){
        paths=new HashMap<>();
        paths.put("/parcial", "application/json");
    }

    private static HttpServer getInstance(){
        return _instance;
    }

    static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 35000; //returns default port if heroku-port isn't set
    }

    public String makeResponse(String path){
        String type = "text/html";
        Path file = Paths.get("./static"+path+".html");
        Charset charset = Charset.forName("ISO-8859-1");
        String outmsg = "";
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                outmsg = outmsg + "\r\n" + line;
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            //x.printStackTrace();
            if(path.equals("/")){
                return makeResponse("/calculadora");
            }
            return makeResponse("/404");
        }
        //System.out.println(type);
        return "HTTP/1.1 200 OK\r\n"
        + "Content-Type: "+type+"\r\n"
        + "\r\n"+outmsg;
    }

    public void processResponse(Socket clientSocket) throws IOException{
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        String inputLine;
        String method="";
        String path="";
        String version="";
        List<String> headers = new ArrayList<String>();
        while ((inputLine = in.readLine()) != null) {
            if(method.isEmpty()){
                String[] requestString = inputLine.split(" ");
                method= requestString[0];
                path = requestString[1];
                version = requestString[2];
                //System.out.println("Request: "+ method + " "+path+" "+version);
            }else{
                //System.out.println("Header: "+inputLine);
                headers.add(inputLine);
            }
            //System.out.println("Received: " + inputLine);
            if (!in.ready()) {
                break;
            }
        }

        out.println(makeResponse(path));

        out.close();

        in.close();
    }

    public void startServer(String[] args) throws IOException{
        int port = getPort();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: "+port);
            System.exit(1);
        }
        Socket clientSocket = null;
        boolean running = true;
        while(running){
            try {
                System.out.println("Listo para recibir en puerto: "+port);
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }

            processResponse(clientSocket);
    
            clientSocket.close();    
        }
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException{
        HttpServer.getInstance().startServer(args);
    }
}
