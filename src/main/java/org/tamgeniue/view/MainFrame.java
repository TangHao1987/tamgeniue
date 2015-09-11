package org.tamgeniue.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;

/**
 * Created by Hao on 7/9/2015.
 * The access of app
 */
@Component("mainFrame")
public class MainFrame extends JFrame{

    @Value("800")
    private int width;

    @Value("800")
    private int height;

    @Autowired
    private GridCanvas gridCanvas;

    public void init(){
        setSize(width, height);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().add(gridCanvas);
        setVisible(true);
    }
}
