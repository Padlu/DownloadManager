/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package download.manager;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
/**
 *
 * @author dell
 */

//This class renders a JProgressBar in a table cell.
public class ProgressRenderer extends JProgressBar implements TableCellRenderer 
{
    //Constructor for ProgressRenderer.
    public ProgressRenderer(int min, int max)
    {
        super(min, max);
    }
    
    //Returns this JProgressBar as the renderer for the given table cell.
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        //Set JProgressBar's percent complete value.
        setValue((int) ((Float) value).floatValue());
        return this;
    }
}
