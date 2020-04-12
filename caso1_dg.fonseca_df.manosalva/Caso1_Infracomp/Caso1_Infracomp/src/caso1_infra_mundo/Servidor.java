package caso1_infra_mundo;

public class Servidor extends Thread 
{

	/**
	 *  Atributo que representa el buffer
	 */
	private Buffer buff;

	/**
	 * Atributo que representa un mensaje
	 */
	private Mensaje msg;

	/**
	 * Atributo que representa el estado del servidor, si se detuvo o esta en ejecución
	 */
	private boolean estado;



	/**
	 * Constructor del servidor
	 * @param pmsg Mensaje que entra como parametro de inicialización
	 * @param pbuff Buffer que entra como parametro de inicialización
	 */
	public Servidor(Buffer pbuff)
	{
		this.buff=pbuff;
		this.msg=new Mensaje(0, null);
		this.estado=true;
	}

	/**
	 * Retorna el mensaje que tiene el servidor
	 * @return msg Mensaje del servidor
	 */
	public Mensaje darMensaje()
	{
		return msg;
	}


	/**
	 * Metodo que actualiza el mensaje del servidor
	 * @param pmensaje
	 */
	public void setMensaje(Mensaje pmensaje)
	{
		this.msg=pmensaje;
	}

	/**
	 * Metodo que detiene el estado del servidor
	 */
	public void detenerServidor()
	{
		if(this.estado) {
			System.out.println("----El servidor se detuvo");
			this.estado=false;}
	}



	/**
	 * Metodo que corre el thread
	 */
	public void run()
	{
		while(estado)
		{
			buff.retirar(this);
			yield();
		}
	}


}
