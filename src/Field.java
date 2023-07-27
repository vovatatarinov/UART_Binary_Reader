import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import jssc.*;
public class Field extends JPanel {

    public class OpenFileFilter extends FileFilter {

        String description = "";
        String fileExt = "";

        public OpenFileFilter(String extension) {
            fileExt = extension;
        }

        public OpenFileFilter(String extension, String typeDescription) {
            fileExt = extension;
            this.description = typeDescription;
        }

        @Override
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            return (f.getName().toLowerCase().endsWith(fileExt));
        }

        @Override
        public String getDescription() {
            return description;
        }
    }
    private static FileOutputStream fileOutputStream;
    static class SerialPortReader implements SerialPortEventListener {
        private SerialPort serialPort;
        private File saveFileName;

        public SerialPortReader(SerialPort serialPort, File saveFileName) {
            this.serialPort = serialPort;
            this.saveFileName = saveFileName;
            try {
                fileOutputStream = new FileOutputStream(saveFileName);
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(null, "Не могу создать файл!");
                try {
                    serialPort.closePort();
                } catch (SerialPortException portException) {
                    JOptionPane.showMessageDialog(null, "Не закрыть порт!");
                }
                return;
            }

        }
        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR()) {
                if (event.getEventValue() > 0) {
                    try {
                        byte buffer[] = serialPort.readBytes();
                        fileOutputStream.write(buffer);
                        fileOutputStream.flush();
                    } catch (SerialPortException ex) {
                        ex.printStackTrace();
                    } catch (IOException IOe) {
                        IOe.printStackTrace();
                    }
                }
            }
        }
    }
    class NorthPanel extends JPanel {
        class TextFieldPanel extends JPanel {
            private JTextField textField;
            public TextFieldPanel() {
                setFocusable(true);
                this.setLayout(new GridLayout(1, 2));

                textField = new JTextField("1000000");
                JTextArea textArea = new JTextArea("Введите скорость (bps)");
                this.add(textArea);
                this.add(textField);
            }
            public String getTextFieldString() {
                return textField.getText();
            }
        }
        private JComboBox comboBox;
        private TextFieldPanel textFieldPanel;
        public NorthPanel() {
            setFocusable(true);
            this.setLayout(new GridLayout(2,1));
            String portList[] = SerialPortList.getPortNames();
            comboBox = new JComboBox(portList);
            textFieldPanel = new TextFieldPanel();
            this.add(comboBox);
            this.add(textFieldPanel);
            if (portList.length == 0) {
                JOptionPane.showMessageDialog(null, "Последовательный порт не найден!");
            }
        }
        public Object getPortNameObj() {
            return comboBox.getSelectedItem();
        }
        public TextFieldPanel getTextFieldPanel() {
            return textFieldPanel;
        }
    }
    private boolean isOpened;
    private SerialPort serialPort;
    public Field(JFrame parent) {
        setFocusable(true);
        this.setLayout(new GridLayout(2,1));
        NorthPanel northPanel = new NorthPanel();
        this.add(northPanel);
        JButton button = new JButton("Начать запись.");
        isOpened = false;
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!isOpened) {
                    isOpened = true;
                    button.setText("Остановить запись");
                    Object portNameObj = northPanel.getPortNameObj();
                    if (portNameObj == null) {
                        JOptionPane.showMessageDialog(null, "Порт не выбран!");
                        return;
                    }

                    String portName = portNameObj.toString();
                    serialPort = new SerialPort(portName);
                    int baudrate = 0;
                    try {
                        baudrate = new Integer(northPanel.getTextFieldPanel().getTextFieldString());
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, "Введено не число!");
                        return;
                    }

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Сохранить файл");

                    fileChooser.removeChoosableFileFilter(fileChooser.getFileFilter());

                    OpenFileFilter binaryFilesFilter = new OpenFileFilter("bin", "Двоичный файл (*.bin)");
                    OpenFileFilter allFiles = new OpenFileFilter("", "Все файлы (*.*)");
                    fileChooser.addChoosableFileFilter(binaryFilesFilter);
                    fileChooser.addChoosableFileFilter(allFiles);
                    fileChooser.setFileFilter(binaryFilesFilter);

                    String currentDir = System.getProperty("user.dir");
                    if (currentDir != null) {
                        fileChooser.setCurrentDirectory(new File(currentDir));
                    }
                    int returnValFileChooser = fileChooser.showSaveDialog(parent);
                    if (returnValFileChooser != JFileChooser.APPROVE_OPTION) {
                        isOpened = false;
                        button.setText("Начать запись.");
                        return;
                    }
                    File saveFileName = fileChooser.getSelectedFile();
                    if (!(fileChooser.getFileFilter().accept(saveFileName))) {
                        String path = saveFileName.getAbsolutePath();
                        path += ".bin";
                        saveFileName = new File(path);
                    }
                    try {
                        serialPort.openPort();
                        serialPort.setParams(baudrate,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        int mask = SerialPort.MASK_RXCHAR;
                        serialPort.setEventsMask(mask);
                        serialPort.addEventListener(new SerialPortReader(serialPort, saveFileName));
                    } catch (SerialPortException e) {
                        JOptionPane.showMessageDialog(null, "Ошибка открытия порта!\n" + e.toString());
                    }
                } else {
                    isOpened = false;
                    button.setText("Начать запись.");
                    try {
                        serialPort.closePort();
                    } catch (SerialPortException portException) {
                        JOptionPane.showMessageDialog(null, "Не закрыть порт!");
                    }
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Не закрыть файл!");
                    }
                }
            }
        });
        this.add(button);

    }
}
