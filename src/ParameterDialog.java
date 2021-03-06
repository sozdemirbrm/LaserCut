import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import static javax.swing.JOptionPane.showMessageDialog;

class ParameterDialog extends JDialog {
  private static final DecimalFormat mmf = new DecimalFormat("#0.0##");    // 0.001 mm resolution
  private static final DecimalFormat inf = new DecimalFormat("#0.0###");   // 0.1 mils
  private boolean   cancelled = true;
  private Point     mouseLoc;
  private double    lblWeight = 0.5;

  static class ParmItem {
    String      name, units = "", hint, key;
    Object      value, valueType;
    JComponent  field;
    boolean     readOnly, lblValue;

    ParmItem (String name, Map<String,String> map, String key, String[] fields) {
      this(name, new BField(fields, Integer.parseInt(map.get(key))));
      this.key = key;
    }

    ParmItem (String name, Map<String,String> map, String key) {
      this(name, map.get(key));
      this.key = key;
    }


    ParmItem (Object value) {
      this.value = value;
    }

    ParmItem (String name, Object value) {
      int idx1 = name.indexOf("{");
      int idx2 = name.indexOf("}");
      if (idx1 >= 0 && idx2 >= 0 && idx2 > idx1) {
        hint = name.substring(idx1 + 1, idx2);
        name = name.replace("{" + hint + "}", "");
      }
      if (name.startsWith("*")) {
        name = name.substring(1);
        readOnly = true;
      } else if (name.startsWith("@")) {
        name = name.substring(1);
        lblValue = true;
      }
      if (name.contains(":")) {
        String[] parts = name.split(":");
        name = parts[0];
        valueType = Arrays.copyOfRange(parts, 1, parts.length);
      } else {
        this.valueType = value;
      }
      String[] tmp = name.split("\\|");
      if (tmp.length == 1) {
        this.name = name;
      } else {
        this.name = tmp[0];
        this.units = tmp[1].toLowerCase();
      }
      if ("boolean".equals(units)) {
        valueType = this.value = (boolean) "1".equals(value);
        units = "";
      } else {
        this.value = value;
      }
    }

    void setValue (Object value) {
      this.value = value;
      if (valueType == null) {
        valueType = value;
      }
    }

    void setField (String value) {
      if (field instanceof JTextField) {
        ((JTextField) field).setText(value != null ? value : "");
      }
    }

    String getStringValue () {
      if (value instanceof Boolean) {
        return (boolean) value ? "1" : "0";
      } else if (value instanceof ParameterDialog.BField) {
        return Integer.toString(((ParameterDialog.BField) value).getValue());
      } else if (value instanceof String) {
        return (String) value;
      } else if (value instanceof Integer) {
        return Integer.toString((int) value);
      } else if (value instanceof Double) {
        return Double.toString((double) value);
      }
      return null;
    }

    // Return true if invalid value
    private boolean setValueAndValidate (String newValue, boolean mmUnits) {
      if (valueType instanceof Integer) {
        try {
          this.value = Integer.parseInt(newValue);
        } catch (NumberFormatException ex) {
          return true;
        }
      } else if (valueType instanceof Double) {
        boolean mmInput = false;
        boolean inInput = false;
        boolean inches = "in".equals(units);
        if (inches) {
          if (newValue.endsWith("mm")) {
            newValue = newValue.substring(0, newValue.length() - 2).trim();
            mmInput = true;
          } else if (newValue.endsWith("in")) {
            newValue = newValue.substring(0, newValue.length() - 2).trim();
            inInput = true;
          }
        }
        try {
          double val;
          if (newValue.contains("/")) {
            // Convert fractional value to decimal
            int idx = newValue.indexOf("/");
            String nom = newValue.substring(0, idx).trim();
            String denom = newValue.substring(idx + 1).trim();
            val =  Double.parseDouble(nom) /  Double.parseDouble(denom);
          } else {
            val =  Double.parseDouble(newValue);
          }
          if (inches) {
            if ((mmInput || mmUnits) && !inInput) {
              val = LaserCut.mmToInches(val);
            }
          }
          this.value = val;
        } catch (NumberFormatException ex) {
          return true;
        }
      } else if (valueType instanceof String[]) {
        value = newValue;
      } else if (valueType instanceof String) {
        value = newValue;
      }
      return false;
    }
  }

  /**
   * Subclass of JComboBox that displays a list of fonts showing the typeface in the component
   */
  class FontList extends JComboBox<String> {
    private Dimension   dim;

    FontList (String[] fonts) {
      super(fonts);
      dim = getPreferredSize();
      int points = 14;
      setSelectedItem(0);
      setFont(new Font((String) getSelectedItem(), Font.PLAIN, points));
      setRenderer(new BasicComboBoxRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          setFont(new Font((String) value, Font.PLAIN, points));
          return this;
        }
      });
      addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          setFont(new Font((String) getSelectedItem(), Font.PLAIN, points));
        }
      });
    }

    @Override
    public Dimension getPreferredSize () {
      return dim == null ? super.getPreferredSize() : dim;
    }
  }


  private void selLabelWeight (double lblWeight) {
    this.lblWeight = lblWeight;
  }

  private GridBagConstraints getGbc (int x, int y) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
    gbc.fill = (x == 0) ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
    gbc.weightx = (x == 0) ? lblWeight : 1.0 - lblWeight;
    gbc.ipady = 2;
    return gbc;
  }

  static String[] getLabels (String[] vals) {
    String[] tmp = new String[vals.length];
    for (int ii = 0; ii < vals.length; ii++) {
      tmp[ii] = vals[ii].contains("|") ? vals[ii].substring(0, vals[ii].indexOf("|")) : vals[ii];
    }
    return tmp;
  }

  static String[] getValues (String[] vals) {
    String[] tmp = new String[vals.length];
    for (int ii = 0; ii < vals.length; ii++) {
      tmp[ii] = vals[ii].contains("|") ? vals[ii].substring(vals[ii].indexOf("|") + 1) : vals[ii];
    }
    return tmp;
  }

  boolean doAction () {
    return !cancelled;
  }

  Point getMouseLoc () {
    return mouseLoc;
  }

  /**
   * Constructor for Pop Up Parameters Dialog with error checking
   * @param parms array of ParmItem objects that describe each parameter
   */
  ParameterDialog (ParmItem[] parms, String[] buttons, boolean mmUnits) {
    this(parms, buttons, mmUnits, null);
  }

  ParameterDialog (ParmItem[] parms, String[] buttons, boolean mmUnits, Properties info) {
    super((Frame) null, true);
    setTitle("Edit Parameters");
    JPanel fields = new JPanel();
    fields.setLayout(new GridBagLayout());
    int jj = 0;
    for (ParmItem parm : parms) {
      if (parm.value instanceof JComponent) {
        if (parm.name != null) {
          fields.add(new JLabel(parm.name + ": "), getGbc(0, jj));
          fields.add((JComponent) parm.value, getGbc(1, jj));
        } else {
          GridBagConstraints gbc = new GridBagConstraints();
          gbc.gridx = 0;
          gbc.weightx = 1;
          gbc.fill = GridBagConstraints.HORIZONTAL;
          gbc.gridwidth = GridBagConstraints.REMAINDER;
          fields.add((JComponent) parm.value, gbc);
        }
        jj++;
        continue;
      }
      boolean inches = parm.units.equals("in");
      fields.add(new JLabel(parm.name + ": "), getGbc(0, jj));
      if (parm.valueType instanceof Boolean) {
        JCheckBox select = new JCheckBox();
        select.setBorderPainted(false);
        select.setFocusable(false);
        select.setBorderPaintedFlat(true);
        select.setSelected((Boolean) parm.value);
        select.setHorizontalAlignment(JCheckBox.RIGHT);
        fields.add(parm.field = select, getGbc(1, jj));
      } else if (parm.valueType instanceof String[]) {
        String[] labels = getLabels((String[]) parm.valueType);
        JComboBox select = parm.name.equals("fontName") ? new FontList(labels) : new JComboBox<>(labels);
        String[] values = getValues((String[]) parm.valueType);
        select.setSelectedIndex(Arrays.asList(values).indexOf((String) parm.value));
        fields.add(parm.field = select, getGbc(1, jj));
      } else if (parm.value instanceof JComponent) {
        fields.add(parm.field = (JComponent) parm.value, getGbc(1, jj));
      } else {
        String val;
        if (parm.value instanceof Double) {
          if (inches && mmUnits) {
            val = LaserCut.df.format(LaserCut.inchesToMM((Double) parm.value));
          } else {
            val = LaserCut.df.format(parm.value);
          }
        } else {
          val = parm.value.toString();
        }
        if (parm.lblValue) {
          // If label name starts with "@" display value as JLabel
          JLabel lbl = new JLabel(val, SwingConstants.RIGHT);
          fields.add(parm.field = lbl, getGbc(1, jj));
        } else {
          JTextField jtf = new JTextField(val, 8);
          Dimension dim = jtf.getPreferredSize();
          jtf.setPreferredSize(new Dimension(dim.width, dim.height - 4));
          jtf.setEditable(!parm.readOnly);
          if (parm.readOnly) {
            jtf.setForeground(Color.gray);
          }
          jtf.setHorizontalAlignment(JTextField.RIGHT);
          fields.add(parm.field = jtf, getGbc(1, jj));
          parm.field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained (FocusEvent ev) {
              super.focusGained(ev);
              // Clear pink background indicating error
              JTextField tf = (JTextField) ev.getComponent();
              tf.setBackground(Color.white);
            }
          });
        }
      }
      int col = 2;
      if (info != null) {
        if (parm.key != null  &&  info.containsKey(parm.key)) {
          try {
            String msg = info.getProperty(parm.key);
            final String[] tmp = msg.split("--");
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/info.png"));
            JButton iBut = new JButton(icon);
            Dimension dim = iBut.getPreferredSize();
            iBut.setPreferredSize(new Dimension(dim.width - 4 , dim.height - 4));
            fields.add(iBut, getGbc(col++, jj));
            iBut.addActionListener(ev -> {
            JTextArea textArea = new JTextArea(15, 40);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            textArea.setText(tmp.length > 1 ? tmp[1] : tmp[0]);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setCaretPosition(0);
            showMessageDialog(this, scrollPane, tmp.length > 1 ? tmp[0] : "Info", JOptionPane.PLAIN_MESSAGE);
            });
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        } else {
          fields.add(new JLabel(""), getGbc(col++, jj));
        }
      }
      if (parm.units != null) {
        fields.add(new JLabel(" " + (inches ? (mmUnits ? "mm" : "in") : parm.units)), getGbc(col, jj));
      }
      if (parm.hint != null) {
        parm.field.setToolTipText(parm.hint);
      } else if (inches && parm.value instanceof Double) {
        if (mmUnits) {
          parm.field.setToolTipText(inf.format(parm.value) + " in");
        } else {
          parm.field.setToolTipText(mmf.format(LaserCut.inchesToMM((Double) parm.value)) + " mm");
        }
      }
      jj++;
    }
    // Define a custion action button so we can catch and save the screen coordinates where the "Place" button was clicked...
    // Yeah, it's a lot of weird code but it avoids having the placed object not show up until the mouse is moved.
    JButton button = new JButton( buttons[0]);
    button.addActionListener(actionEvent -> {
      JButton but = ((JButton) actionEvent.getSource());
      JOptionPane pane = (JOptionPane) but.getParent().getParent();
      pane.setValue(buttons[0]);
      JOptionPane.getRootFrame().dispose();
    });
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed (MouseEvent ev) {
        super.mousePressed(ev);
        Point bl = button.getLocationOnScreen();
        mouseLoc = new Point(bl.x + ev.getX(), bl.y + ev.getY());
      }
    });
    Object[] buts = new Object[] {button, buttons[1]};
    JOptionPane optionPane = new JOptionPane(fields, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, buts, buttons[1]);
    setContentPane(optionPane);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    optionPane.addPropertyChangeListener(ev -> {
      String prop = ev.getPropertyName();
      if (isVisible() && (ev.getSource() == optionPane) && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
          JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
        Object value = optionPane.getValue();
        if (value != JOptionPane.UNINITIALIZED_VALUE) {
          // Reset the JOptionPane's value.  If you don't do this, then if the user
          // presses the same button next time, no property change event will be fired.
          optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
          if (buttons[0].equals(value)) {
            boolean invalid = false;
            for (ParmItem parm : parms) {
              Component comp = parm.field;
              if (comp instanceof JTextField) {
                JTextField tf = (JTextField) comp;
                if (parm.setValueAndValidate(tf.getText(), mmUnits)) {
                  invalid = true;
                  tf.setBackground(Color.pink);
                } else {
                  tf.setBackground(Color.white);
                }
              } else if (comp instanceof JCheckBox) {
                parm.value = (((JCheckBox) comp).isSelected());
              } else if (comp instanceof JComboBox) {
                JComboBox sel = (JComboBox) comp;
                if (parm.valueType instanceof String[]) {
                  String[] values = getValues((String[]) parm.valueType);
                  parm.setValueAndValidate(values[sel.getSelectedIndex()], mmUnits);
                }
              }
            }
            if (!invalid) {
              dispose();
              cancelled = false;
            }
          } else {
            // User closed dialog or clicked cancel
            dispose();
          }
        }
      }
    });
    pack();
    setResizable(false);
  }

  static class BField extends JPanel {
    JCheckBox[] chks;

    BField (String[] fields, int value) {
      setLayout(new GridLayout(1, fields.length));
      chks = new JCheckBox[fields.length];
      for (int ii = 0; ii< chks.length; ii++) {
        String field = fields[ii];
        add(chks[ii] = new JCheckBox(field, (value & (1 << ( chks.length - ii - 1))) != 0));
        JCheckBox chk = chks[ii];
        chk.addActionListener(ev -> {
          boolean selected = !chk.isSelected();
          chk.setSelected(!selected);
        });
      }
    }

    int getValue () {
      int val = 0;
      for (int ii = 0; ii < chks.length; ii++) {
        val |= (chks[ii].isSelected() ? 1 << ( chks.length - ii - 1) : 0);
      }
      return val;
    }
  }

  /**
   * Display parameter edit dialog
   * @param parms Array of ParameterDialog.ParmItem objects initialized with name and value
   * @param parent parent Component (needed to set position on screen)
   * @return true if user pressed OK
   */

  static boolean showSaveCancelParameterDialog (ParmItem[] parms, Component parent) {
    ParameterDialog dialog = (new ParameterDialog(parms, new String[] {"Save", "Cancel"}, false));
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);              // Note: this call invokes dialog
    return dialog.doAction();
  }

  public static void main (String... args) {
    ParmItem[] parmSet = {
        new ParmItem("Ready|boolean", "1"),
        new ParmItem(new JSeparator()),
        new ParmItem("Enabled"),
        new ParmItem("*Power|%{tool tip}", 80),
        new ParmItem("Motor:Nema 8|0:Nema 11|1:Nema 14|2:Nema 17|3:Nema 23|4", "2"),
        new ParmItem("Font:plain:bold:italic", "bold"),
        new ParmItem(new JButton("Full width component")),
        new ParmItem("JButton", new JButton("JButton with label")),
        new ParmItem("Speed", 60),
        new ParmItem("Bit Field", new BField(new String[] {"X", "Y", "Z"}, 5)),
        new ParmItem("@Freq|Hz", 500.123456)};
    if (showSaveCancelParameterDialog(parmSet, null)) {
      for (ParmItem parm : parmSet) {
        System.out.println(parm.name + ": " + parm.value);
      }
    } else {
      System.out.println("Cancel");
    }
  }
}