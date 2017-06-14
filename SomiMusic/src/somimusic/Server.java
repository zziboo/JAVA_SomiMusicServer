package somimusic;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Executors;


public class Server {
    public static void main(String[] args) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(443);
        HttpServer server = HttpServer.create(addr, 0);

        server.createContext("/ads/android", new MyHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }


    static class MyHandler implements HttpHandler {

        private static final String FILE_PATH = "C:/Music/";

        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();

            if (requestMethod.equalsIgnoreCase("GET")) {

                Headers responseHeaders = exchange.getResponseHeaders();

                URI uri = exchange.getRequestURI();

                Object obj = parseQuery(uri.getQuery());
                if (obj == null) {
                    gracefulClose(exchange);
                    return;
                } else if (obj instanceof FileItem) {
                    FileItem target = (FileItem) obj;
                    int fileLength = (int) target.file.length();

                    FileInputStream fileIn = null;
                    byte[] fileData = new byte[fileLength];

                    fileIn = new FileInputStream(target.file);
                    fileIn.read(fileData);

                    fileIn.close();

                    OutputStream responseBody = exchange.getResponseBody();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);

                    responseHeaders.set("Content-Disposition", "attachment; filename=" + target.file.getName());
                    responseHeaders.set("Content-type", "multipart/formed-data");
                    responseHeaders.set("Content-length", fileLength + "");

                    responseBody.write(fileData);
                    responseBody.close();

                    return;
                } else if (obj instanceof String) {
                    String files = (String) obj;
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                    OutputStream responseBody = exchange.getResponseBody();

                    responseHeaders.set("Content-type", "text/plain");
                    responseHeaders.set("Content-length", files.length() + "");
                    responseBody.write(files.getBytes());
                    responseBody.close();
                    return;
                }
            } else if (requestMethod.equalsIgnoreCase("POST")) {

                Headers responseHeader = exchange.getResponseHeaders();
                responseHeader.set("Content_Type", "text/plain");

                exchange.sendResponseHeaders(HttpURLConnection.HTTP_FORBIDDEN, 0);

                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(("refused").getBytes());
                responseBody.close();
            }
        }

        private void gracefulClose(HttpExchange exchange) throws IOException {
            OutputStream responseBody = exchange.getResponseBody();
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
            responseBody.close();
        }

        private Object parseQuery(String query) {
            if (query != null) {
                String pairs[] = query.split("[&]");

                for (String pair : pairs) {
                    String param[] = pair.split("[=]");

                    String key = null;
                    String value = null;

                    if (param.length > 0) {
                        key = param[0];
                    }

                    if (param.length > 1) {
                        value = param[1];
                    }

                    if (key != null && key.equalsIgnoreCase("title")) {
                        return getFile(value);
                    } else if (key != null && key.equalsIgnoreCase("list")) {
                        return getList();
                    }
                }
            }
            return null;
        }

        private FileItem getFile(String fileName) {
            try {
                String decodedString = URLDecoder.decode(fileName, "UTF-8");
                File file = new File(FILE_PATH + decodedString);

                if (!file.isFile()) throw new Exception();

                System.out.println("DOWNLOAD : " + decodedString);
                return new FileItem(file, decodedString);
            } catch (Exception ex) {
                return null;
            }
        }

        private String getList() {
            String files = null;
            try {
                File dir = new File(FILE_PATH);
                File[] fileList = dir.listFiles();

                for (File f : fileList) {
                    if (f.isFile()) {
                        int pos = f.getName().lastIndexOf(".");
                        String ext = f.getName().substring(pos + 1);
                        if (checkAudio(ext)) {
                            if (files == null)
                                files = f.getName();
                            else
                                files += "%" + f.getName();
                        }
                    }
                }
            } catch (Exception e) {
                return null;
            }

            return files;
        }

        private boolean checkAudio(String extension) {
            Vector<String> exts = new Vector<String>();
            exts.add("m4r");
            exts.add("mp3");
            return exts.contains(extension);
        }
    }
}