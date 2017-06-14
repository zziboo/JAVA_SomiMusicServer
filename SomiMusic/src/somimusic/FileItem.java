package somimusic;

import java.io.File;

/**
 * Created by NCS-KSW on 2017-06-13.
 */
public class FileItem {
    public File file;
    public String deocdedString;

    public FileItem(File file, String deocdedString){
        this.file = file;
        this.deocdedString = deocdedString;
    }
}
