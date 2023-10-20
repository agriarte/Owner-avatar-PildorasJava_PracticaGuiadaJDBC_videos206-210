package pildoras.practicajdbc;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class PracticaJDBC {

    public static void main(String[] args) {
        //La aplicación se inicia instanciando la clase MarcoBBDD y haciendola visible.
        //También se define el comportamiento que tiene al salir, generalmente Cerrar
        MarcoBBDD miFrame = new MarcoBBDD();
        miFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        miFrame.setVisible(true);

    }
}

class MarcoBBDD extends JFrame {

    public MarcoBBDD() {
        setBounds(300, 300, 700, 700);

        //Para agregar el panel al frame se crear instancia de clase PanelBBDD y "add"
        PanelBBDD miPanel = new PanelBBDD();
        add(miPanel);
    }
}

class PanelBBDD extends JPanel {

    private JComboBox<String> tablas;
    private JTextArea info;

    Connection miConexion;
    private FileReader miFileReader;//para leer archivo de datos de usuario y contraseña

    public PanelBBDD() {
        setLayout(new BorderLayout());
        tablas = new JComboBox();
        info = new JTextArea();

        add(tablas, BorderLayout.NORTH);
        add(info, BorderLayout.CENTER);

        conectaBBDD();

        nombreTablas();

        //poner JComboBox a la escucha
        tablas.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //se ejecuta al pulsar el combo
                System.out.println(tablas.getSelectedItem());//solo para depuración
                ejecutaConsultaTablas((String) tablas.getSelectedItem());
            }
        });

    }

    public void conectaBBDD() {
        try {
            //La siguiente línea es para evitar problemas de que el jar no encuentre el driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(PanelBBDD.class.getName()).log(Level.SEVERE, null, ex);
        }

        miConexion = null;

        //array de tamaño 3 para recoger Url base de datos, usuario y contraseña
        String datosFichero[] = new String[3];
        //Para mostrar el directorio actual dónde se ejecuta la aplicación
        //System.out.println(System.getProperty("user.dir"));
        try {
            
            // Ruta relativa al directorio de trabajo actual
            File miFichero = new File("confBBDD.txt");
            //Se accede al archivo
            miFileReader = new FileReader(miFichero);
            //vía de comunicación con el archivo
            BufferedReader miBuffer = new BufferedReader(miFileReader);
            //leer línea a línea el archivo. Son 3 líneas. Empezamos por el 0 porque los arrays el primer elemento es 0
            for (int i=0; i<=2 ; i++) {
                datosFichero[i] = miBuffer.readLine();
            }
            
            miConexion = DriverManager.getConnection( datosFichero[0],  datosFichero[1],  datosFichero[2]);

            String nombreBaseDeDatos = miConexion.getCatalog();
            System.out.println("Nombre de la base de datos: " + nombreBaseDeDatos);

        } catch (IOException e) {
           JOptionPane.showMessageDialog(this,"no se ha encontrado el archivo");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void nombreTablas() {
        ResultSet rs = null;

        try {
            //para obtener info de las bases de datos se usa la clase DatabaseMetaData
            DatabaseMetaData metadatosBBDD = miConexion.getMetaData();

            //Con getTables se obtienen los nombres de las tablas. Hay 4 parámetros para filtrar que tablas mostrar. Son 
            //para usar en bases de datos complejas. Con 4 null mostrará todas las tablas existentes.
            //El primer parámetro null especifica el catálogo.Si se establece en null, se obtendrán las tablas de todos 
            //los catálogos (bases de datos) disponibles.
            //El segundo parámetro null especifica el esquema.Si se establece en null, se obtendrán las tablas de todos 
            //los esquemas (normalmente, todos los esquemas en la base de datos).
            //El tercer parámetro null especifica el patrón de nombre de la tabla, se obtendrán todas las tablas sin 
            //ningún patrón de filtro.
            //El cuarto parámetro null especifica los tipos de tablas que deseas recuperar .Si se establece en null, 
            //se obtendrán todos los tipos de tablas.
            //****IMPORTANTE, tuve que filtrar con el primer parámetro añadiendo en el primer parámetro "catálogo 
            //el nombre de la base de datos o de lo contrario me mostraba tablas de la base de datos "mysql" que se instala 
            //por defecto con phpmyadmin o mysql, no se cual, quizás con XAMPP, no lo he averiguado
            rs = metadatosBBDD.getTables("gestionpedidos", null, null, null);

            while (rs.next()) {
                tablas.addItem(rs.getString("TABLE_NAME"));
            }

        } catch (SQLException ex) {
            Logger.getLogger(PanelBBDD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void ejecutaConsultaTablas(String nombreTabla) {

        //Para almacenar los nombres de los campos de la tabla usamos ArrayList<String>
        //Es un array de tipo string que permite crecer en tiempo de ejecución
        ArrayList<String> nCampos = new ArrayList<>();

        String consulta = "SELECT * FROM " + nombreTabla;

        try {
            Statement miSt = miConexion.createStatement();
            ResultSet miRs = miSt.executeQuery(consulta);

            //Para averiguar los metadatos de la BD se usa getMetaData() sobre el ResultSet.
            //Devuelve un objeto de tipo ResulSetMetaData  
            ResultSetMetaData rsBD = miRs.getMetaData();
            //Sobre el objeto rsBD obtenido se llama a los métodos para obtener el número de columnas(campos) y sus nombres
            //ATENCION: la primera columna es la 1. Si por error iniciamos for con i=0 dará error de "Column out of range"
            for (int i = 1; i < rsBD.getColumnCount(); i++) {
                nCampos.add(rsBD.getColumnName(i));
            }

            //borrar el JText "info" antes de cada consulta
            info.setText(" ");
            //recorrer el ResultSet miRs para obtener los registros
            while (miRs.next()) {
                //Con un bocle foreach recorremos al ArrayList para obtener los campos. Por cada campo se hace un append 
                //en cada registro.
                for (String infoCampo : nCampos) {
                    info.append(miRs.getString(infoCampo) + " ");
                }
                info.append("\n");
            }

        } catch (SQLException ex) {
            Logger.getLogger(PanelBBDD.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
