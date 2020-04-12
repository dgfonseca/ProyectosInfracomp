package caso1_infra_mundo;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

public class Buffer 
{
	/**
	 * Atributo que representa los mensajes en el buffer
	 */
	private ArrayList<Mensaje> mensajes= new ArrayList<>();

	/**
	 * Atributo que representa los servidores en el buffer
	 */
	private static ArrayList<Servidor> servers= new ArrayList<>();

	/**
	 * Atributo que representa el tamaño del buffer
	 */
	private int n;



	/**
	 * Metodo que retorna la lista de servidores
	 * @return servers
	 */
	public ArrayList<Servidor> darServidor()
	{
		return servers;
	}


	/**
	 * Constructor del buffer
	 * @param tamaño del buffer
	 */
	public Buffer(int tamaño)
	{
		this.n=tamaño;
		this.mensajes=new ArrayList<Mensaje>();

	}


	/**
	 * Metodo que almacena un mensaje en el buffer
	 * @param msg Mensaje que el cliente almacena en el buffer
	 */
	public void almacenar(Mensaje msg)
	{
		while(mensajes.size()>=n)
		{
			synchronized(msg.darCliente())
			{
				try 
				{
					System.out.println("2)---El cliente: "+msg.darCliente().darIdentificación()+" intenta el  wait en el metodo almacenar");
					msg.darCliente().darBuffer().wait();
				} 
				catch (InterruptedException e) 
				{
					System.out.println("EROOR EN EL WAIT DE ALMACENAR"+e.getMessage());

				}
			}
		}


		synchronized (this) {
			System.out.println("3)---El cliente: "+msg.darCliente().darIdentificación()+" Agrega el mensaje al buffer");
			mensajes.add(msg);
			try 
			{
				System.out.println("4)---El cliente: "+msg.darCliente().darIdentificación()+" intenta hacer el wait en espera de la respuesta de un servidor");
				msg.darCliente().darBuffer().wait();
			} 
			catch (InterruptedException e) 
			{
				System.out.println("ERRORE EN EL WAIT DE ALMACENAR 2.0"+e.getMessage());			
			}
		}

	}

	/**
	 * Metodo en el que el servidor retira un mensaje del buffer y lo modifica.
	 * @param server Servidor que retira el mensaje del buffer
	 */
	public void retirar(Servidor server)
	{
		synchronized (server) {


			while(mensajes.size()!=0)
			{
				Mensaje m=mensajes.remove(0);
				m.aumentarNumero();
				server.setMensaje(m);
				synchronized (this) {
					server.darMensaje().darCliente().darBuffer().notify();
					notifyAll();
					System.out.println("6)---El servidor retira el mensaje y lo modifica para devolverlo al cliente: "+m.darCliente().darIdentificación());
				}
			}

		}


	}

	/**
	 * Metodo que detiene el servidor cuando el cliente
	 * termina el proceso
	 */
	public void detenerServidor()
	{


		for(int i=0;i<servers.size();i++)
		{
			Servidor server= servers.get(i);
			synchronized (server) {


				if(mensajes.size()==0)
				{
					try {
						Thread.sleep(50);
						if(mensajes.size()==0) {
							server.detenerServidor();}
						else notifyAll();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}




	public static void main(String[] args){

		
		
		//Carga del Archivo
		File arc = new File("./data/arch.properties");
        Properties datos = new Properties( );  
        try
        {
        	FileInputStream in = new FileInputStream( arc );
            datos.load( in );
            in.close( );
        }
        catch( Exception e )
        {
            System.err.println("No se encontrÃ³ el archivo con los datos iniciales");
        }

        Integer numServ = Integer.parseInt(datos.getProperty("numServidores"));
        Integer numClientes = Integer.parseInt(datos.getProperty("numClientes"));
        Integer numMensajes = Integer.parseInt(datos.getProperty("numMensajes"));
        Integer tamBuffer = Integer.parseInt(datos.getProperty("tamBuffer"));
		Buffer buff = new Buffer(tamBuffer);
		for(int i=0;i<numClientes;i++)
		{
			Cliente client= new Cliente(i,numMensajes, buff);
			client.start();
		}
		for (int i=0;i<numServ;i++)
		{

			Servidor server= new Servidor(buff);
			servers.add(server);
			server.start();
		}

	}


}
