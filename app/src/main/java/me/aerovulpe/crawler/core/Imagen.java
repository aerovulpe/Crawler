package me.aerovulpe.crawler.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import me.aerovulpe.crawler.utils.SomeUtilities;


/**
 *
 * @author Carlos3dx http://www.carlos3dx.net
 * @version 1.2
 */
public class Imagen {
    
    private ArrayList<String> uris=new ArrayList<>(); /** Arraylist where 
            the differents URIs of the image are placed */
    private String folder="";   //Folder in server
    private String fileName="";
    private String extension="";
    private ArrayList<String> listMD5;
    private int position=0;
    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH-mm-ss_";
    
    /**
     * Constructor of Imagen
     * 
     * @param uri URI of image
     * @param sizes ArrayList with tag sizes which will be downloaded <b>IMPORTANT:</b>
     * sizes from largest to smallest
     * @param bestSize If it's true, only will be downloaded the largest image
     * @param hashesMD5 ArrayList with all hashes of files in destination folder
     */
    public Imagen(String uri, ArrayList<String> sizes, boolean bestSize,
            boolean untagged, ArrayList<String> hashesMD5, boolean avatar){
        listMD5=hashesMD5;
        String[] aux= uri.split("/");  
        fileName=aux[aux.length-1].split("\\.")[0]; // Get Filename from URI
        extension=aux[aux.length-1].split("\\.")[1];// Get Filename extension from URI
        folder=uri.split(fileName)[0];              // Get the folder where is the image 
        //if(fileName.contains("tumblr_")){
            if (fileName.contains("avatar") || uri.contains("default_avatar")){
                if (!avatar) {
                    return;
                }
            }
            String auxFileName =fileName;
            int fin=fileName.lastIndexOf('_'); //Get the positition of the last '_' in the filename
            //System.out.println(""+fin);
            if(fin>6){
                /* Obtain The root of filename without tag size (_xxx) */
                //System.out.println(fileName);
                fileName=fileName.substring(0, fin);
                //System.out.println(fileName);
                for(int i=0;i<sizes.size();i++){
                    /* Make a URI for each tag and check if exists in server */
                    String auxUri=folder+fileName+sizes.get(i)+"."+extension;
                    //System.out.println(auxUri);
                    if(SomeUtilities.isImage(auxUri)){
                        uris.add(auxUri);
                        if(bestSize){
                            break;
                        }
                    }
                }
            //}
            if(this.isEmpty() && untagged){
                String auxUri=folder+auxFileName+"."+extension;
                    //System.out.println(auxUri);
                    if(SomeUtilities.isImage(auxUri)){
                        uris.add(auxUri);
                    }
            }
            
        }
            
    }
    
   
    /**
     * Check if the object has any URI for download the image
     * 
     * @return True if there are no URIs
     */
    public boolean isEmpty(){
        return (uris.isEmpty());
    }
    
    /**
     * 
     * @return Number of URIs in the object
     */
    public int size(){
        return uris.size();
    }
    
    /**
     * Move to first URI
     * @return True if OK, false if not
     */
    public boolean moveToFirst(){
        if(uris.isEmpty())
        {
            return false;
        }
        else
        {
            position=0;
            return true;
        }
    }
    
    /**
     * Move to last URI
     * @return True if OK, false if not
     */
    public boolean moveToLast(){
        if(uris.isEmpty())
        {
            return false;
        }
        else
        {
            position=uris.size()-1;
            return true;
        }
    }
    
    /**
     * Move to next URI
     * @return True if OK, false if not
     */
    public boolean moveNext(){
        if(uris.isEmpty() || position>=uris.size()-1)
        {
            return false;
        }
        else
        {
            position++;
            return true;
        }
    }
    
    /**
     * Download the image at current position
     * @param dest Destination folder
     * @param ifExists Action if file already exists 0 Skip, 1 owerwrite, 2 add currenttimestamp
     * @param persName personaliced name for the file, null for original name
     * @return Log about the result
     */
    public String download(String dest,int ifExists, String persName){
        String[] aux= uris.get(position).split("/"); 
        String auxFileName=(persName!=null)?persName:aux[aux.length-1]; 
        String log=aux[aux.length-1];
        boolean continuar=true;
        
        if(existFile(uris.get(position),dest+auxFileName)){
            if(ifExists==0){
                continuar=false;
                log+=" already exists";
            } 
            else if(ifExists==2)
            {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
                auxFileName=sdf.format(cal.getTime())+auxFileName;
            }
        }
        
        if(continuar){
            int attempts=0;
            while(attempts<5){
                try{
                    InputStream is = new URL(uris.get(position)).openStream();
                    OutputStream os = new FileOutputStream(dest+auxFileName);

                    byte[] b = new byte[2048];
                    int length;

                    while ((length = is.read(b)) != -1) 
                    {
                        os.write(b, 0, length);
                    }

                    is.close();
                    os.close();

                    log+=" saved";
                    attempts=5;
                }
                catch (Exception e){
                    if(++attempts==5){
                        log+=" an error ocurred";
                    }
                }
            }
        }
        
        return log;
    }
    
    
    private boolean existFile(String url,String path){
        File f = new File(path);
        //Check if file exists
        if(f.exists())
        {
            return true;
        }
        //Check if file exists with other name
        if(listMD5!=null)
        {
            int attempts=0;
            while(attempts <5)
            try 
            {
                String fileMD5=SomeUtilities.getMD5Checksum(url,false);
                for (int i=0;i<listMD5.size();i++)
                {
                    if(fileMD5.equals(listMD5.get(i)))
                    {
                        return true;
                    }
                }
                listMD5.add(fileMD5); 
                attempts=5;
            }catch (ConnectException e) 
            {
                if(++attempts==5){
                    e.printStackTrace();
                }
            }
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    /*
     * Return the MD5 List with the new hashes
     */
    public ArrayList<String> getMD5(){
        return listMD5;
    }

}
