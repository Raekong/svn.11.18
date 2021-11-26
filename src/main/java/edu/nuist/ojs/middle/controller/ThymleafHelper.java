package edu.nuist.ojs.middle.controller;

import javax.servlet.http.HttpSession;

import org.apache.catalina.Contained;
import org.springframework.ui.Model;

import edu.nuist.ojs.middle.context.Context;
import edu.nuist.ojs.middle.interceptor.ContextInterceptor;

public class ThymleafHelper {
    public static void home(Model m, HttpSession session) throws Exception{
        String[] sessionkeys = {
            "i18n",
            "email",
            "superUser",
            "name",
            "abbr"
        };
        String[] modelkeys = {
            "lang",
            "email",
            "isSuper",
            "publisher",
            "publisherAbbr"
        };
        
        set(m, session, sessionkeys, modelkeys);
    }

    public static void set(Model m, HttpSession session, String[] sessionkeys, String[] modelkeys) throws Exception{
        int index = 0;
        for(String key : sessionkeys){
            Object o = get(session, key, Object.class);
            if( o == null ) throw new Exception("set for thmyleaf parameter error, "+ key + " is null");
            //System.out.println( modelkeys[index] +"*****************************"+ o );
            m.addAttribute( modelkeys[index], o);
            index++;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(HttpSession session, String key, Class<T> c){
        return (T)session.getAttribute(key);
    }

    public static Context getContext(HttpSession session){
        return (Context)session.getAttribute(ContextInterceptor.CONTEXT);
    }
}
