import java.util.*;
import java.io.*;

public class GenericJsonParser {

 public Hashtable rootTable;
 public Vector rootVector;

 public Stack objectStack = new Stack();
 public Stack keyStack = new Stack();
 boolean afterColon=false;

 boolean quoteOn=false;
 boolean escapeNext=false;
 StringBuffer buff = new StringBuffer();
 StringBuffer hexBuff = new StringBuffer();

 int openSquareBracketCount=0;
 int openCurlyBracketCount=0;
 int fourDigitHexCodeCount=-1;

 public void addToBuff(char c) {
   buff.append(c);
 }

 public void handleQuoteOn(char c) {

   if (fourDigitHexCodeCount > -1) {
     hexBuff.append(c);
     fourDigitHexCodeCount++;

     if (fourDigitHexCodeCount == 4) {
       addToBuff((char)Integer.parseInt(hexBuff.toString(), 16));
       fourDigitHexCodeCount = -1;
     }
     return;
   }

   switch (c) {
     case '"':
       if (escapeNext) {
         escapeNext = false;
         addToBuff(c);
       }
       else {
         quoteOn = false;
       }
       break;
     case '\\':
       if (escapeNext) {
         escapeNext = false;
         addToBuff(c);
       }
       else
         escapeNext = true;
       break;
     case 'u':
       if (escapeNext) {
         fourDigitHexCodeCount = 0;
         hexBuff.setLength(0);
         escapeNext = false;
       } else
         addToBuff(c);
       break;
     case 'n':
       if (escapeNext) {
         addToBuff('\n');
         escapeNext = false;
       } else
         addToBuff(c);
       break;
     default:
       addToBuff(c);
       escapeNext = false;
   }
 }

 public void handleQuoteOff(char c) {
   switch (c) {
     case '"':
       quoteOn = true;
       break;
     case '{':
       Hashtable table = new Hashtable();
       if (rootVector == null && rootTable == null)
         rootTable = table;
       objectStack.push(table);
       afterColon=false;
       break;
     case '}':
       table = (Hashtable)objectStack.pop();

       if (afterColon)
         table.put(keyStack.pop(), buff.toString());
       buff.setLength(0);

       if (!objectStack.empty()) {
         Object object = objectStack.peek();
         if (object instanceof Hashtable)
           ((Hashtable)object).put(keyStack.pop(), table);
         else
           ((Vector)object).addElement(table);
       }
       afterColon=false;
       break;
     case '[':
       Vector vector = new Vector();
       if (rootVector == null && rootTable == null)
         rootVector = vector;
       objectStack.push(vector);
       afterColon = true;
       break;
     case ']':
       vector = (Vector)objectStack.pop();

       if (buff.length() > 0)
         vector.addElement(buff.toString());
       buff.setLength(0);

       if (!objectStack.empty()) {
         Object object = objectStack.peek();
         if (object instanceof Hashtable)
           ((Hashtable)object).put(keyStack.pop(), vector);
         else
           ((Vector)object).addElement(vector);
       }
       afterColon = false;
       break;
     case ':':
       keyStack.push(buff.toString());
       buff.setLength(0);
       afterColon = true;
       break;
     case ',':
       if (!afterColon)
         return;

       Object object = objectStack.peek();
       if (object instanceof Hashtable)
         ((Hashtable)object).put(keyStack.pop(), buff.toString());
       else
         ((Vector)object).addElement(buff.toString());

       buff.setLength(0);
       break;
     case ' ':
       break;
     case '\t':
       break;
     case '\n':
       break;
     case '\r':
       break;
     default:
       addToBuff(c);
   }
 }

 public void handleStream(InputStream is) throws Exception {
   try {
     int ch;
     while ((ch = is.read()) != -1) {
       char c = (char)ch;
       if (quoteOn)
         handleQuoteOn(c);
       else
         handleQuoteOff(c);
     }
   }
   finally {
     is.close();
   }
 }
 
 public static void main(String[] args) throws Exception {
   System.out.println("start");

   GenericJsonParser jp = new GenericJsonParser();
   jp.handleStream(new FileInputStream("demo.json"));
   System.out.println(jp.rootVector+"");
   System.out.println(jp.rootTable+"");
 }

}
