package caso1_infra_mundo;

public class Cliente extends Thread 
{

	/**
	 * Identificador del cliente
	 */
	private int identificacion;

	/**
	 * Vector de mensajes
	 */
	private Mensaje[] msgs;

	/**
	 * Buffer donde van los mensajes
	 */
	private Buffer buff;


	/**
	 * Constructor de un cliente
	 * @param pid identificador del cliente
	 * @param pMensajes Cantidad de mensajes del cliente
	 * @param pbuff Buffer para almacenar mensajes
	 */
	public Cliente(int pid, int pMensajes, Buffer pbuff)
	{
		this.msgs=new Mensaje[pMensajes];
		this.identificacion=pid;
		this.buff=pbuff;
	}


	/**
	 * Metodo que retorna la identificacion
	 * @return identificacion
	 */
	public int darIdentificación()
	{
		return identificacion;
	}


	/**
	 * Metodo que retorna el buffer
	 * @return buff
	 */
	public Buffer darBuffer()
	{
		return buff;
	}


	/**
	 * Metodo run de la clase Thread
	 */
	public void run()
	{
		for(int i=0;i<msgs.length;i++)
		{
			Mensaje msg=new Mensaje(i, this);
			msgs[i]=msg;
			System.out.println("1)---El cliente :"+ this.identificacion+" Creo un mensaje:"+msg.darNumero()+" Y lo almaceno");
			buff.almacenar(msg);
		}
		buff.detenerServidor();


	}

}
