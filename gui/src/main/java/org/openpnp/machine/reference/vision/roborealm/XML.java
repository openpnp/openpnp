package org.openpnp.machine.reference.vision.roborealm;


import java.util.Hashtable;
import java.util.Vector;

/* The following XML class is a simple XML class meant to process the primitive XML
 * that comes from RoboRealm. This class can be removed and replaced with a more
 * extensive XML processing class as needed but is guaranteed to work with the RR
 * XML. Do NOT use this class for generic XML processing as it is included for
 * completeness and is intentionally kept simplistic to ease understanding
 * */

class XML
{
  Hashtable <String, String>table = new Hashtable<String, String>();
  Vector <String>list = new Vector<String>();

  private String replace(String txt, String src, String dest)
  {
    if (txt==null) return new String("");
    int i,j;
    int len=src.length();
    StringBuffer sb=new StringBuffer(txt.length());

    j=0;
    while ((i=txt.indexOf(src,j))>=0)
    {
        sb.append(txt.substring(j,i));
        sb.append(dest);
        i+=len;
        j=i;
    }
    sb.append(txt.substring(j));

    return sb.toString();
  }

  /*
  Unescapes strings that have been included in an XML message. This can be
  accomplished by a sequence of replace statements.
    &amp; -> &
    &quote; -> "
    &lt; -> <
    &gt; -> >
  */
  private String unescape(String txt)
  {
    replace(txt, "&amp;", "&");
    replace(txt, "&quote;", "\"");
    replace(txt, "&lt;", "<");
    replace(txt, "&gt;", ">");
    return txt;
  }

  public boolean parse(String s)
  {
    table.clear();
    return parse(s, table, null);
  }

  public Vector parseVector(String s)
  {
    list.removeAllElements();
    if (parse(s, null, list))
      return list;
    else
      return null;
  }

  public boolean parse(String s, Hashtable <String, String>h, Vector <String>v)
  {
    boolean isEndTag;
    byte txt[] =  s.getBytes();
    int i, j;
    int len = s.length();
    StringBuffer keys[] = new StringBuffer[10];
    StringBuffer value = new StringBuffer();
    for (i=0;i<10;i++)
      keys[i] = new StringBuffer();
    int keyTop=-1;

    for (i=0;i<len;)
    {
      // read in key
      if (txt[i]=='<')
      {
        i++;
        if (txt[i]=='/')
        {
          isEndTag = true;
          i++;
        }
        else
          isEndTag = false;

        keyTop++;
        keys[keyTop].setLength(0);
        while ((i<len)&&(txt[i]!='>'))
        {
          keys[keyTop].append((char)txt[i]);
          i++;
        }
        if (txt[i++]!='>')
        {
          System.out.println("Missing close > tag");
          return false;
        }

        if (isEndTag)
        {
          if (!keys[keyTop].toString().equals(keys[keyTop-1].toString()))
          {
            System.out.println("Mismatched XML tags "+keys[keyTop]+" -> "+keys[keyTop-1]);
            return false;
          }
          keyTop-=2;
        }
      }
      else
      {
        // read in value
        value.setLength(0);

        while ((i<len)&&(txt[i]!='<'))
        {
          value.append((char)txt[i]);
          i++;
        }

        StringBuffer key = new StringBuffer();
        for (j=0;j<=keyTop;j++)
        {
          if (j>0) key.append('.');
          key.append(keys[j]);
        }

        String escapedValue = unescape(value.toString());
        if (h!=null) h.put(key.toString(), escapedValue);
        if (v!=null) v.addElement(escapedValue);
      }
    }

    return true;
  }

  public int getInt(String txt)
  {
    String s = (String)table.get(txt);
    if (s!=null)
    {
      return Integer.parseInt(s);
    }
    return 0;
  }

  public String getFirst()
  {
    if (table.isEmpty())
      return null;
    else
      return (String)table.elements().nextElement();
  }
/*
  // This is where the program first starts
  public static void main(String[] args)
  {
    XML xml = new XML();
    xml.parse("<response><width>100</width><height>200</height></response>");
    System.out.println(xml.getInt("response.width"));
    System.out.println(xml.getInt("response.height"));
  }
*/
}
