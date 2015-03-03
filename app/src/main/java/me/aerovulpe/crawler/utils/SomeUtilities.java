
package me.aerovulpe.crawler.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

/**
 * Some methods for the application
 * @author Carlos3dx http://www.carlos3dx.net
 * @version 1.0
 */
public class SomeUtilities {
    
    /**
     * Connects to the server requesting the file, if it exists, return 200,
     * any other code is an error
     * @param uri URI to resource
     * @return True if exists
     */
    public static boolean existsFileInServer(String uri){
        boolean exists=false;
        
        try{
            URL url = new URL (uri);
            URLConnection connection = url.openConnection();

            connection.connect();

            // Cast to a HttpURLConnection
            if ( connection instanceof HttpURLConnection)
            {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                if(httpConnection.getResponseCode()==200){
                    exists=true;
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return exists;
    }
    
    /**
     * Check if the element of the URI is an image file.
     * @param uri URI of the image
     * @return Trie if it is or false if it isn't
     */
    public static boolean isImage(String uri){
        boolean isImage=false;
        if(existsFileInServer(uri)){ //Before trying to read the file, ask if resource exists
            try{
                byte[] bytes=getBytesFromFile(uri); //Array of bytes
                String hex =bytesToHex(bytes); 
                if(hex.substring(0, 32).equals("89504E470D0A1A0A0000000D49484452")){
                    isImage=true;
                }
                else if(hex.startsWith("89504E470D0A1A0A0000000D49484452") || // PNG Image 
                        hex.startsWith("47494638") || // GIF8
                        hex.startsWith("474946383761") || // GIF87a
                        hex.startsWith("474946383961") || // GIF89a
                        hex.startsWith("FFD8FF") // JPG
                        ){
                    isImage=true;
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        return isImage;
    }
    
    /**
     * Conver the array to a string of Hex values.
     * @param bytes Array of Bytes to process
     * @return 
     */
    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    /**
     * Get the MD5 Checksum from a file
     * @param path String with the path to file
     * @return
     * @throws Exception 
     */
    public static String getMD5Checksum(String path) throws Exception {
        return getMD5Checksum(path,true);
    }
    
    /**
     * Get the MD5 Checksum from a file
     * @param path String with the path to file
     * @param file True if the file is in the computer and false if the path is an URI
     * @return
     * @throws Exception 
     */
    public static String getMD5Checksum(String path,boolean file) throws Exception{
        InputStream fis;
        if(file)
        {
            fis= new FileInputStream(path);
        }
        else
        {
            fis= new URL(path).openStream();
        }

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        byte[] b =complete.digest();
        String result = "";

        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
    
    
    public static byte[] getBytesFromFile(String uri) throws IOException {
        byte[] bytes;
        try (InputStream is = new URL(uri).openStream()) {
            int length = 32;
            bytes = new byte[length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read the file");
            }
        }
        return bytes;
    }

}
