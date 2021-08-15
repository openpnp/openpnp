package org.I18n;

import java.util.HashMap;
import java.lang.*;
import java.io.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Locale;

public class I18n {
    static HashMap<String,String> o_map;
    private static Gson gson = new Gson();

    public static String readFileContent(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }

    public static void i18ninit()
    {
        
        Locale locale = Locale.getDefault();
        System.out.println("Systeam Language:" + locale.getLanguage());
        String json_text = readFileContent("i18n\\" + locale.getLanguage() + ".json");
        o_map = gson.fromJson(json_text, new TypeToken<HashMap<String,String>>() {}.getType());
    }

    public static String getException(Exception e) {
        Writer writer = null;
        PrintWriter printWriter = null;
        try {
            writer = new StringWriter();
            printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            return writer.toString();
        } finally {
            try {
                if (writer != null)
                {
                    writer.close();
                }
                    
                if (printWriter != null)
                {
                    printWriter.close();
                }
                    
            } catch (IOException e1) { }
        }
    }
    
    public static String gettext(String text)
    {
        
        try {
            String s = o_map.get(text);
            if(s != null)
            {
                return s;
            }
            else
            {
                return text;
            }
            
        } catch (Exception e)
        {
            System.out.println(getException(e));
            return text;
        }
        //o_json.getString(text);
    }
}
