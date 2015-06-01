package ksami.gpsservicetest;

import android.util.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//Data structure
class KmaData
{
    String area;
    String day;
    String wfEn;
    String hour;
    String temp;
    String tmn;
    String tmx;
}
public class XmlParser2
{
    public static void main(String[] args){

        int gridx,gridy;
        gridx = 59;
        gridy = 125;
        //TODO : make gridx,gridy input interface

        parsing(gridx, gridy);

    }
    public static ArrayList<KmaData> parsing(int gridx,int gridy){
    
        String rssFeed = "http://www.kma.go.kr/wid/queryDFS.jsp?gridx=%s&gridy=%s";
        
        String url = String.format(rssFeed, gridx, gridy);
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        ArrayList<KmaData> kmaList = new ArrayList<KmaData>();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(url);

//            //area name
//            NodeList areaName = doc.getElementsByTagName("category");
//            for (int i = 0; i < areaName.getLength(); i++) {
//                Node dataNode = areaName.item(i);
//                if(dataNode.getNodeType()==Node.ELEMENT_NODE) {
//                    NodeList children = dataNode.getChildNodes();
//                    for (int j=0; j<children.getLength(); j++) {
//                        Node childNode = children.item(j);
//                        System.out.println(childNode.getNodeValue());
//                    }
//
//                }
//            }

            NodeList bodyList = doc.getElementsByTagName("data");   //split using data tag "<data> ... </data>"
            
            for (int i = 0; i < bodyList.getLength(); i++)
            {
                Node dataNode = bodyList.item(i);
                Node Temp = dataNode.getNextSibling();
                while (true)
                {
                    Temp = Temp.getNextSibling();
                         
                    if (Temp == null)
                        break;
                    if (Temp.getNodeType() == Node.ELEMENT_NODE)
                    {
                        NodeList dataList = Temp.getChildNodes();
                        KmaData kmaTemp = new KmaData();
                        kmaTemp.area = "";
                        for (int j = 0; j < dataList.getLength(); j++)
                        {
                            Node data = dataList.item(j);
                            if (data.getNodeType() == Node.ELEMENT_NODE)
                            {
                                switch(data.getNodeName())
                                {
                                    
                                    case "day":
                                        kmaTemp.day = data.getChildNodes().item(0).getNodeValue();
                                        break;
                                    case "wfEn":
                                        kmaTemp.wfEn = data.getChildNodes().item(0).getNodeValue();
                                        break;
                                    case "hour":                                                    
                                        kmaTemp.hour = data.getChildNodes().item(0).getNodeValue();   
                                        break;
                                    case "temp":
                                        kmaTemp.temp = data.getChildNodes().item(0).getNodeValue();
                                        break;
                                    case "tmn":
                                        kmaTemp.tmn = data.getChildNodes().item(0).getNodeValue();
                                        break;
                                    case "tmx":
                                        kmaTemp.tmx = data.getChildNodes().item(0).getNodeValue();
                                        break;
                                }
                            }
                        }
                        kmaList.add(kmaTemp);
                        kmaTemp = null;
                    }
                }
            }

            for (int i = 0; i < bodyList.getLength()-1; i++)  //print result
            {
                System.out.print(i + " ");
                System.out.print("/day " + kmaList.get(i).day);

                System.out.print("/wfEn " +kmaList.get(i).wfEn + " ");
//                System.out.print("/tmn " +kmaList.get(i).tmn + " ");
//                System.out.print("/tmx " +kmaList.get(i).tmx + " ");
                System.out.print("/temp " +kmaList.get(i).temp + " ");

                System.out.print("/hour " +kmaList.get(i).hour + " ");
                System.out.println();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return kmaList;
    }
}
