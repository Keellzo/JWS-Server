import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Server {
    public static void main(String[] args) {

        String filepath = "C:\\Users\\kelly\\Desktop\\Grit22\\JWS\\Server\\src\\data.json";

        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(8080);
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

        while (true) {
            try {
                Socket socket = serverSocket.accept();

                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

                String requestLine = bufferedReader.readLine();

                int contentLength = -1;
                String message;

                while (true) {
                    message = bufferedReader.readLine();
                    if (message == null || message.trim().isEmpty()) {
                        break;
                    }

                    if (message.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(message.substring("Content-Length:".length()).trim());
                    }
                }

                if (requestLine.startsWith("GET")) {
                    JSONParser parser = new JSONParser();
                    JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(filepath));

                    if (requestLine.contains("/petsBySpecies")) {
                        String species = requestLine.split("species=")[1].split(" ")[0];
                        JSONArray filteredArray = new JSONArray();
                        for (Object obj : jsonArray) {
                            JSONObject jsonObject = (JSONObject) obj;
                            if (species.equalsIgnoreCase((String) jsonObject.get("species"))) {
                                filteredArray.add(jsonObject);
                            }
                        }
                        jsonArray = filteredArray;
                    }

                    String response = jsonArray.toJSONString();
                    bufferedWriter.write("HTTP/1.1 200 OK\r\n");
                    bufferedWriter.write("Content-Type: application/json\r\n");
                    bufferedWriter.write("Content-Length: " + response.length() + "\r\n");
                    bufferedWriter.write("Connection: keep-alive\r\n");
                    bufferedWriter.write("\r\n");
                    bufferedWriter.write(response);
                } else if (requestLine.startsWith("POST")) {
                    StringBuilder payloadBuilder = new StringBuilder();
                    if (contentLength > 0) {
                        char[] contentBuffer = new char[contentLength];
                        bufferedReader.read(contentBuffer);
                        payloadBuilder.append(new String(contentBuffer));
                    }

                    String payload = payloadBuilder.toString();
                    JSONParser parser = new JSONParser();
                    JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(filepath));
                    JSONObject jsonObject = (JSONObject) parser.parse(payload);
                    jsonArray.add(jsonObject);
                    FileWriter fileWriter = new FileWriter(filepath);
                    fileWriter.write(jsonArray.toJSONString());
                    fileWriter.close();
                    bufferedWriter.write("HTTP/1.1 201 Created\r\n");
                    bufferedWriter.write("Content-Length: 0\r\n");
                    bufferedWriter.write("Connection: close\r\n");
                    bufferedWriter.write("\r\n");
                } else {
                    bufferedWriter.write("HTTP/1.1 400 Bad Request\r\n");
                    bufferedWriter.write("Content-Length: 0\r\n");
                    bufferedWriter.write("Connection: close\r\n");
                    bufferedWriter.write("\r\n");
                }

                bufferedWriter.flush();
                socket.close();
                inputStreamReader.close();
                outputStreamWriter.close();
                bufferedReader.close();
                bufferedWriter.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
