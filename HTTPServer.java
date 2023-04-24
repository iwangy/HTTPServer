import java.io.*;
import java.net.*;
import java.nio.file.*;

public class HTTPServer {

  static int port = 80;
  private static final String BASE_DIRECTORY = "./";
  private static final String DEFAULT_MIME_TYPE = "text/plain";

  public static void main(String[] args) throws IOException {
    ServerSocket server = new ServerSocket(port);
    System.out.println("Listening for connection on port " + port);

    while (true) {
      try {
        Socket clientSocket = server.accept();
        System.out.println("Client Connected");

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  
        String line = reader.readLine();

        String method = line.split(" ", 3)[0];
        String path = line.split(" ", 3)[1];
        String response = "";
                
        if ("GET".equalsIgnoreCase(method)) {
          response = handleGetRequest(path);
        } else if ("POST".equalsIgnoreCase(method)) {
          // response = handlePostRequest(path, body);
          response = handlePostRequest(path, reader);
        } else if ("PUT".equalsIgnoreCase(method)) {
          // response = handlePutRequest(path, body);
          response = handlePutRequest(path, reader);
        } else if ("DELETE".equalsIgnoreCase(method)) {
          response = handleDeleteRequest(path);
        } else {
          response = "HTTP/1.1 400 Bad Request\r\nUnsupported HTTP method";
        }
        
        System.out.println(response + "\r\nRESPONSE");
        OutputStream outputStream = clientSocket.getOutputStream();
        outputStream.write(response.getBytes());
        outputStream.flush();

        System.out.println("Client Disconnected");
        clientSocket.close();
        reader.close();
      } catch (IOException e) {
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private static String handleGetRequest(String path) throws IOException {
    Path filePath = Paths.get(BASE_DIRECTORY + path);
    System.out.println("GET REQ");

    if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
        String mimeType = getMimeType(filePath);
        byte[] fileContent = Files.readAllBytes(filePath);
        String response = "HTTP/1.1 200 OK\r\nContent-Type: " + mimeType + "\r\nContent-Length: " + fileContent.length + "\r\n\r\n";
        return response + new String(fileContent);
    } else {
        return "HTTP/1.1 404 Not Found\r\nFile not found";
    }
  }

  private static String handlePostRequest(String path, BufferedReader reader) throws IOException {
    Path filePath = Paths.get(BASE_DIRECTORY + path);
    System.out.println("POST REQ");

    if(!Files.exists(filePath)) {
      Files.createDirectories(filePath.getParent());
      Files.createFile(filePath);
    }

    String mimeType = getMimeType(filePath);
    if (mimeType.equals("text/plain")) {
      BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
      String line;
      String body = "";
      while((line = reader.readLine()) != null) {
        if(line.contains("Content-Length")) {
          break;
        } 
      }
      int cLength = Integer.valueOf(line.split(" ")[1]);
      reader.readLine();
      for (int i = 0, c = 0; i < cLength; i++) {
        c = reader.read();
        body += (char)c;
      }
      writer.write(body);
      writer.flush();
      writer.close();
      String response = "HTTP/1.1 200 OK\r\nContent-Type: " + mimeType + "\r\nContent-Length: " + body.length() + "\r\n\r\n";
      return response + body;
    } else {
      return "HTTP/1.1 400 Bad Request\r\nUnsupported MIME type for POST request";
    }
  }

  private static String handlePutRequest(String path, BufferedReader reader) throws IOException {
    Path filePath = Paths.get(BASE_DIRECTORY + path);
    System.out.println("PUT REQ");
    String mimeType = getMimeType(filePath);
    if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));
        String line;
        String body = "";
        while((line = reader.readLine()) != null) {
          if(line.contains("Content-Length")) {
            break;
          } 
        }
        int cLength = Integer.valueOf(line.split(" ")[1]);
        reader.readLine();
        for (int i = 0, c = 0; i < cLength; i++) {
          c = reader.read();
          body += (char)c;
        }
        writer.write(body);
        writer.flush();
        writer.close();
        String response = "HTTP/1.1 200 OK\r\nContent-Type: " + mimeType + "\r\nContent-Length: " + body.length() + "\r\n\r\n";
        return response + body;
    } else {
        return "HTTP/1.1 404 Not Found\r\n\r\nFile not found";
    }
  }

  private static String handleDeleteRequest(String path) throws IOException {
    Path filePath = Paths.get(BASE_DIRECTORY + path);
    System.out.println("DELETE REQ");
    if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
      Files.delete(filePath);
      return "HTTP/1.1 200 OK\r\n\r\nFile updated successfully";
    } else {
      return "HTTP/1.1 404 Not Found\r\n\r\nFile not found";
    }
  }
  
  private static String getMimeType(Path filePath) throws IOException {
    String fileName = filePath.getFileName().toString();
    String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
    String mimeType = DEFAULT_MIME_TYPE;

    if (fileExtension.equalsIgnoreCase("txt")) {
        mimeType = "text/plain";
    } else if (fileExtension.equalsIgnoreCase("html")) {
        mimeType = "text/html";
    } else if (fileExtension.equalsIgnoreCase("json")) {
        mimeType = "application/json";
    }
    return mimeType;
  } 
}

