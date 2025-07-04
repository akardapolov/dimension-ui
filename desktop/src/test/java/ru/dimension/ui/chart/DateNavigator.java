package ru.dimension.ui.chart;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateNavigator extends JPanel {

  private JSlider daySlider;
  private JLabel dateLabel;
  private JLabel dayOfWeekLabel;
  private JButton prevDayButton;
  private JButton nextDayButton;
  private JButton prevWeekButton;
  private JButton nextWeekButton;
  private Calendar currentDate;

  public DateNavigator() {
    currentDate = Calendar.getInstance();
    setLayout(new BorderLayout());

    // Buttons
    prevDayButton = createButton("«", -1);
    nextDayButton = createButton("»", 1);
    prevWeekButton = createButton("‹", -7);
    nextWeekButton = createButton("›", 7);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(prevWeekButton);
    buttonPanel.add(prevDayButton);
    buttonPanel.add(nextDayButton);
    buttonPanel.add(nextWeekButton);
    add(buttonPanel, BorderLayout.WEST);

    // Slider
    daySlider = new JSlider(0, 365, 0); // Adjust range as needed

    // Date and Day of Week Labels
    dateLabel = new JLabel();
    dayOfWeekLabel = new JLabel();
    updateDateLabels();

    JPanel labelPanel = new JPanel(new GridLayout(2,1));
    labelPanel.add(dateLabel);
    labelPanel.add(dayOfWeekLabel);
    add(labelPanel,BorderLayout.CENTER);

    daySlider.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (!daySlider.getValueIsAdjusting()) {
          int daysToAdd = daySlider.getValue() - (daySlider.getMaximum()/2);
          Calendar tempDate = (Calendar) currentDate.clone();
          tempDate.add(Calendar.DAY_OF_YEAR, daysToAdd);
          currentDate = tempDate;
          updateDateLabels();
        }
      }
    });
    add(daySlider, BorderLayout.SOUTH);
    daySlider.setMajorTickSpacing(7);
    daySlider.setPaintTicks(true);
    daySlider.setPaintLabels(true);

    //Initial Slider position
    daySlider.setValue(daySlider.getMaximum()/2);

    setPreferredSize(new Dimension(500, 100)); // Adjust preferred size as needed
  }


  private JButton createButton(String text, int daysToAdd) {
    JButton button = new JButton(text);
    button.addActionListener(e -> {
      currentDate.add(Calendar.DAY_OF_YEAR, daysToAdd);
      updateDateLabels();
      daySlider.setValue(daysToAdd + daySlider.getValue()); // Keep slider in sync
    });
    return button;
  }

  private void updateDateLabels() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    dateLabel.setText(dateFormat.format(currentDate.getTime()));

    SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEE, d");
    dayOfWeekLabel.setText(dayOfWeekFormat.format(currentDate.getTime()));

    //adjust slider position accordingly
    int daysFromStart = (int) ((currentDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis())/ (1000 * 60 * 60*24));
    daySlider.setValue(daySlider.getMaximum()/2 + daysFromStart);
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame("Date Navigator");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(new DateNavigator(), BorderLayout.NORTH);
    frame.setSize(600, 400);
    frame.setVisible(true);
  }
}

