package org.tamgeniue;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.tamgeniue.view.MainFrame;

import javax.swing.*;

/**
 * Created by Hao on 7/9/2015.
 *
 */
public class Application {
    public static void main(String[] args) {
        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("Spring-Module.xml");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MainFrame mainFrame = (MainFrame) ctx.getBean("mainFrame");
                mainFrame.init();
            }
        });
    }
}
