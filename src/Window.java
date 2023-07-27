import javax.swing.*;

public class Window extends JFrame {
    public Window() {
        super("UART Binary reader");
        setBounds(10, 10, 400, 125);

        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Field field = new Field(this);
        getContentPane().add(field);
        setVisible(true);
    }
}
