   URL url = null;

    List> list = new ArrayList>();
    String urlPath = "http://www.kma.go.kr/wid/queryDFS.jsp?gridx=%s&gridy=%s";
    
    try 
    {
        Map map = null;
    
        url = new URL(String.format(urlPath, gridx, gridy));
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        
        factory.setNamespaceAware(true);
        
        XmlPullParser xpp = factory.newPullParser();
        InputStream stream = url.openStream();
        
        xpp.setInput(stream, "UTF-8");
        
        int eventType = xpp.getEventType();
        int listIndex = -1;
        String mapName= null;
        
        while (eventType != XmlPullParser.END_DOCUMENT) 
        {
            switch (eventType) 
            {
                case XmlPullParser.START_DOCUMENT:
                    break;
                    
                case XmlPullParser.END_DOCUMENT:
                    break;
                    
                case XmlPullParser.START_TAG:
                
                    if ("data".equals(xpp.getName())) 
                    {
                        listIndex = Integer.parseInt( xpp.getAttributeValue(0) );
                        map = new HashMap();
                    }
        
                    if(listIndex > -1) 
                    {
                        mapName = xpp.getName();
                        eventType = xpp.next();
                        map.put(mapName, "data".equals(mapName)?listIndex:xpp.getText()); 
                    }
                    break;
                
                case XmlPullParser.END_TAG:
                    if ("data".equals(xpp.getName())) 
                    {
                        list.add(map);
                    }
                    break;
                    
                case XmlPullParser.TEXT:
                    break;
            }
            eventType = xpp.next();
        }
    } 
    catch (Exception e) 
    {
        e.printStackTrace();
    }


