import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.util.regex.Pattern;

// Any developer using WindowBuilder will cause the translations properties file to be normalised.
// That is, property string are written in sorted order, and some characters converted to backslash
// escapes. This makes for a confusing and bloated git history if another developer has made changes
// using other tools which did not preserve this normalised form.
//
// This test helps keep a clean git history by ensuring that the file is always in normalised form.
//
// For developers using WindowBuilder this test is expected to always pass.
//
// For developers using other tools this test will report a failure if the file is not
// normalised, even if it is otherwise correct. Unfortunately these developers need to carry the
// burder of WindowBuilder making spurious changes to the file. A fixed normalised file is written,
// and the developer can make this test pass with:
//
//    mv ./src/main/resources/org/openpnp/translations.properties-normalised ./src/main/resources/org/openpnp/translations.properties
//

public class LocalisationTest {
    @Test
    public void propertiesFileIsNormalised() throws Exception {
        testNormalised("./src/main/resources/org/openpnp/translations.properties");
    }

    private void testNormalised(String filename) throws Exception {
        String normalisedFilename = filename + "-normalised";

        // our Properties object will keep its keys sorted
        Properties properties = new Properties() {
        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
                Set<Map.Entry<Object, Object>> sortedSet = new TreeSet<Map.Entry<Object, Object>>(new Comparator<Map.Entry<Object, Object>>() {
                    @Override
                    public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
                        return o1.getKey().toString().compareTo(o2.getKey().toString());
                    }
                }
                );
                sortedSet.addAll(super.entrySet());
                return sortedSet;
            }
        };

        // Read the original file body into a string, and separately remember any comments
        String originalFileBody = "";
        String comments = "";
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        while (reader.ready()) {
            String line = reader.readLine();
            if (line.startsWith("#")) {
                comments += line + "\n";
            } else {
                originalFileBody += line + "\n";
            }
        }

        // load the original file into the properties, then dump it back out again into a string
        properties.load(new StringReader(originalFileBody));
        String newComments = "";
        StringWriter normalisedFileBodyWriter = new StringWriter();
        properties.store(normalisedFileBodyWriter,newComments);

        // Remove comments from the normalised file too
        String normalisedFileBody = normalisedFileBodyWriter.toString();
        normalisedFileBody = Pattern.compile("^#.*\n",Pattern.MULTILINE).matcher(normalisedFileBody).replaceAll("");

        // If the files are different, then the original was not in normalised form
        if(! normalisedFileBody.equals(originalFileBody))
        {
            String message = new String();
            message += "This file containing translated text is not in normalised form:\n";
            message += filename+"\n";
            message += "You can correct this by copying the corrected file from:\n";
            message += normalisedFilename+"\n";
            message += "For more information see https://github.com/openpnp/openpnp/wiki/Developers-Guide#translations\n";

            FileWriter f = new FileWriter(normalisedFilename);
            f.write(comments+normalisedFileBody);
            f.close();

            throw new Exception(message);
        }
    }
}
