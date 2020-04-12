package caso1_infra_mundo;

public class Mensaje 
{

	private int numeroInformacion;
	
	private Cliente cliente;

	public Mensaje(int pnumero,Cliente pcliente)
	{
		this.numeroInformacion=pnumero;
		this.cliente=pcliente;
	}

	public int darNumero()
	{
		return numeroInformacion;
	}
	
	public Cliente darCliente()
	{
		return cliente;
	}
	
	public void aumentarNumero()
	{
		numeroInformacion+=100;
	}
	


}
