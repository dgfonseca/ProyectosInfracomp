package Servidor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class ThreadDelServidor implements Runnable {

	public static final String OK = "OK";
	public static final String ALGORITMOS = "ALGORITMOS";
	public static final String CERTSRV = "CERTSRV";
	public static final String CERCLNT = "CERCLNT";
	public static final String SEPARADOR = ":";
	public static final String HOLA = "HOLA";
	public static final String INICIO = "INICIO";
	public static final String ERROR = "ERROR";
	public static final String REC = "recibio-";
	public static final int numCadenas = 8;

	// Atributos
	private Socket sc = null;
	private String dlg;
	private byte[] mybyte;
	private static File file;
	private static X509Certificate certSer;
	private static KeyPair keyPairServidor;
	private int idThread;


	public static void init(X509Certificate pCertSer, KeyPair pKeyPairServidor, File pFile) {
		certSer = pCertSer;
		keyPairServidor = pKeyPairServidor;
		file = pFile;
		
	}

	public ThreadDelServidor (Socket csP, int idP) {
		sc = csP;
		dlg = new String("delegado " + idP + ": ");
		idThread=idP;
		try {
			mybyte = new byte[520]; 
			mybyte = certSer.getEncoded();
		} catch (Exception e) {
			System.out.println("Error creando encoded del certificado para el thread" + dlg);
			e.printStackTrace();
		}
	}

	private boolean validoAlgHMAC(String nombre) {
		return ((nombre.equals(SeguridadYCertificado.HMACMD5) || 
				nombre.equals(SeguridadYCertificado.HMACSHA1) ||
				nombre.equals(SeguridadYCertificado.HMACSHA256) ||
				nombre.equals(SeguridadYCertificado.HMACSHA384) ||
				nombre.equals(SeguridadYCertificado.HMACSHA512)
				));
	}

	/*
	 * Generacion del archivo log. 
	 * Nota: 
	 * - Debe conservar el metodo como está. 
	 * - Es el único metodo permitido para escribir en el log.
	 */
	private void escribirMensaje(String pCadena) {

		try {
			FileWriter fw = new FileWriter(file,true);
			fw.write(pCadena + "\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run() {
		String[] cadenas;
		cadenas = new String[numCadenas];

		String linea;
		System.out.println(dlg + "Empezando atencion.");
		try {

			PrintWriter ac = new PrintWriter(sc.getOutputStream() , true);
			BufferedReader dc = new BufferedReader(new InputStreamReader(sc.getInputStream()));

			/***** Fase 1:  *****/
			linea = dc.readLine();
			cadenas[0] = "Fase1: ";
			if (!linea.equals(HOLA)) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			} else {
				ac.println(OK);
				cadenas[0] = dlg + REC + linea + "-continuando.";
				System.out.println(cadenas[0]);
			}

			/***** Fase 2:  *****/
			linea = dc.readLine();
			cadenas[1] = "Fase2: ";
			if (!(linea.contains(SEPARADOR) && linea.split(SEPARADOR)[0].equals(ALGORITMOS))) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			}

			String[] algoritmos = linea.split(SEPARADOR);
			if (!algoritmos[1].equals(SeguridadYCertificado.DES) && !algoritmos[1].equals(SeguridadYCertificado.AES) &&
					!algoritmos[1].equals(SeguridadYCertificado.BLOWFISH) && !algoritmos[1].equals(SeguridadYCertificado.RC4)){
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "Alg.Simetrico" + REC + algoritmos + "-terminando.");
			}
			if (!algoritmos[2].equals(SeguridadYCertificado.RSA) ) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "Alg.Asimetrico." + REC + algoritmos + "-terminando.");
			}
			if (!validoAlgHMAC(algoritmos[3])) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "AlgHash." + REC + algoritmos + "-terminando.");
			}
			cadenas[1] = dlg + REC + linea + "-continuando.";
			System.out.println(cadenas[1]);
			ac.println(OK);

			/***** Fase 3:  *****/
			String testCert = toHexString(mybyte);
			ac.println(testCert);
			cadenas[2] = dlg + "envio certificado del servidor. continuando.";
			System.out.println(cadenas[2] + testCert);				

			/***** Fase 4: *****/
			cadenas[3] = "";
			linea = dc.readLine();
			byte[] llaveSimetrica = SeguridadYCertificado.ad(
					toByteArray(linea), 
					keyPairServidor.getPrivate(), algoritmos[2] );
			SecretKey simetrica = new SecretKeySpec(llaveSimetrica, 0, llaveSimetrica.length, algoritmos[1]);
			cadenas[3] = dlg + "recibio y creo llave simetrica. continuando.";
			System.out.println(cadenas[3]);

			/***** Fase 5:  *****/
			cadenas[4]="";
			linea = dc.readLine();
			System.out.println(dlg + "Recibio reto del cliente:-" + linea + "-");
			byte[] retoByte = toByteArray(linea);
			byte [ ] ciphertext1 = SeguridadYCertificado.se(retoByte, simetrica, algoritmos[1]);
			ac.println(toHexString(ciphertext1));
			System.out.println(dlg + "envio reto cifrado con llave simetrica al cliente. continuado.");

			linea = dc.readLine();
			if ((linea.equals(OK))) {
				cadenas[4] = dlg + "recibio confirmacion del cliente:"+ linea +"-continuado.";
				System.out.println(cadenas[4]);
			} else {
				sc.close();
				throw new Exception(dlg + ERROR + "en confirmacion de llave simetrica." + REC + "-terminando.");
			}

			/***** Fase 6:  *****/
			linea = dc.readLine();				
			byte[] ccByte = SeguridadYCertificado.sd(
					toByteArray(linea), simetrica, algoritmos[1]);
			String cc = toHexString(ccByte);
			System.out.println(dlg + "recibio cc y descifro:-" + cc + "-continuado.");

			linea = dc.readLine();				
			byte[] claveByte = SeguridadYCertificado.sd(
					toByteArray(linea), simetrica, algoritmos[1]);
			String clave = toHexString(claveByte);
			System.out.println(dlg + "recibio clave y descifro:-" + clave + "-continuado.");
			cadenas[5] = dlg + "recibio cc y clave - continuando";

			Random rand = new Random(); 
			int valor = rand.nextInt(1000000);
			String strvalor = valor+"";
			while (strvalor.length()%4!=0) strvalor += 0;
			byte[] valorByte = toByteArray(strvalor);
			byte [ ] ciphertext2 = SeguridadYCertificado.se(valorByte, simetrica, algoritmos[1]);
			ac.println(toHexString(ciphertext2));
			cadenas[6] = dlg + "envio valor "+strvalor+" cifrado con llave simetrica al cliente. continuado.";
			System.out.println(cadenas[6]);

			byte [] hmac = SeguridadYCertificado.hdg(valorByte, simetrica, algoritmos[3]);
			byte[] recibo = SeguridadYCertificado.ae(hmac, keyPairServidor.getPrivate(), algoritmos[2]);
			ac.println(toHexString(recibo));
			System.out.println(dlg + "envio hmac cifrado con llave privada del servidor. continuado.");

			cadenas[7] = "";
			linea = dc.readLine();	
			if (linea.equals(OK)) {
				cadenas[7] = dlg + "Terminando exitosamente." + linea;
				System.out.println(cadenas[7]);
			} else {
				cadenas[7] = dlg + "Terminando con error" + linea;
				System.out.println(cadenas[7]);
			}
			sc.close();

			for (int i=0;i<numCadenas;i++) {
				escribirMensaje(cadenas[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printBase64Binary(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseBase64Binary(s);
	}

}
