package caso;

import java.security.InvalidKeyException;
import java.security.Key;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.x509.X509V3CertificateGenerator;


@SuppressWarnings("deprecation")
public class Cliente 
{
	//Mensajes Servidor/Cliente
	public static final String HOLA = "HOLA";
	public static final String ERROR = "ERROR";	
	public static final String ALGORITMOS="ALGORITMOS";
	public static final String OK = "OK";

	//Puerto
	public static final int PUERTO = 10002;
	public static final String LOCALHOST="localhost";

	//Llaves
	//Asimetricas
	public static final String RSA = "RSA";
	//Simetricas
	public static final String AES = "AES";
	public static final String BLOWFISH = "Blowfish";
	//HMAC
	public static final String HMACSHA1 = "HMACSHA1";
	public static final String HMACSHA256 = "HMACSHA256";
	public static final String HMACSHA384 = "HMACSHA384";
	public static final String HMACSHA512 = "HMACSHA512";

	private String algAsimetrica;
	private String algSimetrica;
	private String Hmac;
	private Socket hilo;
	private PrintWriter out;
	private BufferedReader in;
	private KeyPair lasLlaves;
	private PublicKey llavePublica;
	private SecretKey llaveSecreta;
	@SuppressWarnings("unused")
	private X509Certificate certificado;

	//DATOS

	public Cliente(String simetrica, String asimetrica, String hmac)
	{
		try {
			this.algAsimetrica=asimetrica;
			this.algSimetrica=simetrica;
			this.Hmac=hmac;
			this.lasLlaves=generarLlaves();

			conect();
			protocolo();
			obtenerCertificado();
			generarLlave(algSimetrica);
			enviarLlaveSimetricaYReto();
			autentificarCliente();
			validaciónDeRespuesta();

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
				out.close();
				hilo.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public void conect()
	{
		try {
			this.hilo=new Socket(LOCALHOST,PUERTO);
			this.in=new BufferedReader(new InputStreamReader(hilo.getInputStream()));
			this.out=new PrintWriter(hilo.getOutputStream(),true);
		}
		catch (Exception e) {
			System.err.println("ERROR CONEXION");
			e.printStackTrace();		
		}
	}


	///ETAPA 1//////
	/**
	 * Metodo de la etapa 1: seleccionar algoritmos e iniciar sesión
	 * @throws Exception
	 */
	public void protocolo() throws Exception
	{
		String linea = "";
		out.println(HOLA);
		System.out.println("HOLA ENVIADO");

		linea=in.readLine();
		System.out.println(linea+" RECIBIDO");

		if(linea.equals(ERROR)||!linea.equals(OK))
		{
			System.err.println("Hubo un error de comunicación" + linea);
		}

		out.println(ALGORITMOS+":"+algSimetrica+":"+algAsimetrica+":"+Hmac);
		linea=in.readLine();
		System.out.println("Algoritmo RECIBIDO "+ linea);
		if(linea.equals(ERROR)||!linea.equals(OK))
		{
			System.out.println("No sirven los algoritmos "+ linea);
		}
	}

	///ETAPA 2///

	/**
	 * Metodo que genera el certificado que envia el servidor
	 * @param keyPair2
	 * @return
	 */
	private X509Certificate generarCertificado(KeyPair keyPair2) {
		X509Certificate cert = null;

		X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
		X500Principal prin = new X500Principal("CN=Grupo MPG");
		gen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
		gen.setIssuerDN(prin);
		gen.setNotBefore(new Date(System.currentTimeMillis() - 50000));
		gen.setNotAfter(new Date(System.currentTimeMillis() - 50000));
		gen.setSubjectDN(prin);
		gen.setPublicKey(keyPair2.getPublic());
		gen.setSignatureAlgorithm("MD2withRSA");
		try {
			cert = gen.generate(keyPair2.getPrivate());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cert;
	}

	/**
	 * Metodo que genera las llaves y es el autenticador del servidor.
	 */
	private void obtenerCertificado() {

		certificado = generarCertificado(lasLlaves);

		try {

			String cert = in.readLine();
			byte[] esto = parseBase64Binary(cert);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream is = new ByteArrayInputStream(esto);
			X509Certificate certServer = (X509Certificate) cf.generateCertificate(is);

			llavePublica = certServer.getPublicKey(); 
			System.out.println("Llave Publica obtenida");
		} catch (Exception e) {

			e.printStackTrace();
		}
	}



	//ETAPA 3///

	/**
	 * Metodo que genera las llaves asimetricas
	 * @return llaves
	 */
	private KeyPair generarLlaves() 
	{
		KeyPair llaves = null;

		KeyPairGenerator generador;
		try {generador = KeyPairGenerator.getInstance(algAsimetrica);
		generador.initialize(1024);
		llaves = generador.generateKeyPair();


		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}		

		return llaves;
	}


	/**
	 * Metodo que genera y envia la llave simetrica mediante la llave asimetrica publica del servidor.
	 */
	private void enviarLlaveSimetricaYReto() {
		String reto = "reto";

		try {
			llaveSecreta = generarLlave(algSimetrica);
			byte[] llaveSim=llaveSecreta.getEncoded();
			byte[] simetrica = encriptarAsimetrico(llaveSim, llavePublica, algAsimetrica);
			out.println(printBase64Binary(simetrica));
			out.println(reto);
			String linea = in.readLine();
			byte[] msg = parseBase64Binary(linea);
			byte[] msgDecifrado = decriptadoSimetrico(msg, llaveSecreta, algSimetrica);
			System.out.println("Este es el mensaje descifrado:" +printBase64Binary(msgDecifrado));
			if(!printBase64Binary(msgDecifrado).equals("reto"))
			{
				System.out.println("Error al decifrar el mensaje");
				out.println(ERROR);
			}
			out.println(OK);
		} catch (Exception e) {
			System.out.println("Error generando llave simetrica");
			e.printStackTrace();
		}

	}


	///PARTE 3///

	/**
	 * Metodo que autentica el cliente mediante la cedula y y su contraseña
	 */
	private void autentificarCliente()
	{
		try 
		{
			System.out.println("Ingresar Cedula");
			Scanner entrada=new Scanner(System.in);
			String cadena=entrada.nextLine();

			byte[] ccByte = encriptadoSimetrico(parseBase64Binary(cadena), llaveSecreta, algSimetrica);
			out.println(printBase64Binary(ccByte));

			System.out.println("Ingresar Contraseña");
			String cadena2=entrada.nextLine();
			byte[] claveByte= encriptadoSimetrico(parseBase64Binary(cadena2), llaveSecreta, algSimetrica);
			out.println(printBase64Binary(claveByte));
			entrada.close();

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	//Parte 4

	/**
	 * Metodo que autentica el servidor y el cliente mediante el HMAC
	 */
	private void validaciónDeRespuesta()
	{
		try {

			String valorCifrado = in.readLine();
			String hmacCifrado = in.readLine();
			byte[] valorDecifrado = decriptadoSimetrico(parseBase64Binary(valorCifrado), llaveSecreta, algSimetrica);
			String elString = printBase64Binary(valorDecifrado);
			byte[] elByte = parseBase64Binary(elString);

			System.out.println( "Valor decifrado: "+elString + "$");

			byte[] hmacDescifrado = decriptarAsimetrico(parseBase64Binary(hmacCifrado), llavePublica, algAsimetrica);

			String hmacRecibido =printBase64Binary(hmacDescifrado);
			byte[] hmac = hash(elByte, llaveSecreta, HMACSHA512);
			String hmacGenerado = printBase64Binary(hmac);




			boolean x= hmacRecibido.equals(hmacGenerado);
			if(x!=true)
			{
				out.println(ERROR);
				System.out.println("Error, no es el mismo HMAC");
			}
			else {
				System.out.println("Se confirmo que el mensaje recibido proviene del servidor mediante el HMAC");
				out.println(OK);
			}




		} catch (Exception e) {
			System.out.println("Error con el hash");
			e.printStackTrace();
		}
	}





	private static byte[] encriptarAsimetrico(byte[] msg, Key llave, String x) throws Exception
	{
		Cipher decifrador = Cipher.getInstance(x);
		decifrador.init(1,  llave);
		return decifrador.doFinal(msg);
	}

	public static byte[] decriptarAsimetrico(byte[] msg, Key key, String A) throws Exception
	{
		Cipher decifrador = Cipher.getInstance(A);
		decifrador.init(2,  key);
		return decifrador.doFinal(msg);
	}




	public static void main(String[] args) 
	{
		new Cliente(BLOWFISH, RSA, HMACSHA512);
	}

	public static String printBase64Binary(final byte[] msg)
	{
		return	DatatypeConverter.printBase64Binary(msg);
	}

	public static byte[] parseBase64Binary(final String msg)
	{
		return DatatypeConverter.parseBase64Binary(msg);
	}

	public boolean compararBytes(byte[] a, byte[]b){
		if(a.length!=b.length)
		{
			return false;
		}
		for (int i = 0; i < b.length; i++) {
			if(a[i]!=b[i])
				return false;
		}
		return true;
	}

	public static byte[] encriptadoSimetrico(byte[] msg, Key key, String A)throws Exception
	{
		if( A.equals(AES))
		{
			A = AES+"/ECB/PKCS5Padding";
		}
		else
		{
			A= "Blowfish";
		}
		Cipher decifrador = Cipher.getInstance(A);
		decifrador.init(Cipher.ENCRYPT_MODE, key);
		return decifrador.doFinal(msg);
	}

	public static byte[] decriptadoSimetrico(byte[] msg, Key key, String A)throws Exception
	{
		if( A.equals(AES))
		{
			A = AES+"/ECB/PKCS5Padding";
		}
		else
		{
			A= "Blowfish";
		}
		Cipher decifrador = Cipher.getInstance(A);
		decifrador.init(Cipher.DECRYPT_MODE, key);
		return decifrador.doFinal(msg);
	}

	public static SecretKey generarLlave(final String algoritmo) throws Exception {
		int tamLlave = 0;
		if (algoritmo.equals("DES")) {
			tamLlave = 64;
		}
		else if (algoritmo.equals("AES")) {
			tamLlave = 128;
		}
		else if (algoritmo.equals("Blowfish")) {
			tamLlave = 128;
		}
		else if (algoritmo.equals("RC4")) {
			tamLlave = 128;
		}
		if (tamLlave == 0) {
			throw new NoSuchAlgorithmException();
		}
		final KeyGenerator keyGen = KeyGenerator.getInstance(algoritmo);
		keyGen.init(tamLlave);
		final SecretKey key = keyGen.generateKey();
		return key;
	}

	public static byte[] hash(final byte[] msg, final Key key, final String algo) throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {
		final Mac mac = Mac.getInstance(algo);
		mac.init(key);
		final byte[] bytes = mac.doFinal(msg);
		return bytes;
	}






}
