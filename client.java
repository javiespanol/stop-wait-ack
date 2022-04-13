import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.file.*;
import java.util.*;
import java.util.TreeMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ro2021send {

   public static void main(String[] args) throws IOException, InterruptedException {

      if (args.length != 5) {
         System.out.println("La sintaxis correcta es: input_file dest_IP dest_port emulator_IP emulator_port");
      } else {

         long tiempoEnviado = 0;
         long tiempoRecibido = 0;
         int RTO = 50;
         double tiempoMedia = 0;
         double tiempoVariacion = 0;

         Map<Integer, byte[]> mapa = new TreeMap<>();

         Path fileLocation = Paths.get(args[0]);
         byte[] data = Files.readAllBytes(fileLocation);

         Integer salir = 0;
         Integer numberRecibido = 0;
         Integer numeroPaquetes = (int) Math.ceil(data.length / 1458.0);
         Integer numeroSecuencia = 0;

         for (int m = 0; m < numeroPaquetes - 1; m++) {
            mapa.put(m, Arrays.copyOfRange(data, m * 1458, (m + 1) * 1458));
         }
         mapa.put(numeroPaquetes - 1, Arrays.copyOfRange(data, (numeroPaquetes - 1) * 1458, data.length));

         String puertoC = args[2];
         Short pC = Short.parseShort(puertoC);
         String puertoB = args[4];
         Short pB = Short.parseShort(puertoB);

         DatagramChannel socketChannel = DatagramChannel.open();
         InetSocketAddress hA = new InetSocketAddress(args[3], pB);
         Selector selector = Selector.open();

         socketChannel.configureBlocking(false);
         socketChannel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
         socketChannel.connect(hA);

         while (true) {
            int select = selector.select(RTO);

            if (select > 0) {

               Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

               while (iterator.hasNext()) {

                  SelectionKey selectionKey = iterator.next();

                  if (selectionKey.isReadable()) {

                     socketChannel = (DatagramChannel) selectionKey.channel();
                     ByteBuffer buffer = ByteBuffer.allocate(1500);
                     socketChannel.read(buffer);

                     buffer.flip();
                     int limits = buffer.limit();
                     byte bytes[] = new byte[limits];
                     buffer.get(bytes, 0, limits);

                     numberRecibido = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 6, 10)).order(ByteOrder.BIG_ENDIAN).getInt();

                     long tiempo = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 10, 14)).order(ByteOrder.BIG_ENDIAN).getInt();
                     tiempoRecibido = System.currentTimeMillis();

                     int rtt = (int) (tiempoRecibido - tiempo);

                     if (numberRecibido - 1 == 0) {
                        tiempoMedia = (double) rtt;
                        tiempoVariacion = ((double) rtt) / 2.0;
                     } else {
                        tiempoMedia = (1.0 - 0.125) * tiempoMedia + (0.125) * ((double) rtt);
                        tiempoVariacion = (1.0 - 0.25) * tiempoVariacion+ 0.25 * Math.abs(tiempoMedia - ((double) rtt));
                     }
                     RTO = (int) (Math.ceil(tiempoMedia + 4.0 * tiempoVariacion + 3.0));
                     
                     if(numberRecibido-1>=numeroSecuencia){
                        if (numberRecibido == numeroSecuencia) {
                           numeroSecuencia++;
                        } else {
                           numeroSecuencia = numberRecibido;
                        }
                        

                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                     }
                     buffer.clear();

                  } else if (selectionKey.isWritable()) {

                     if (numeroSecuencia >= numeroPaquetes) {
                        socketChannel = (DatagramChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1500);

                        buffer.put(InetAddress.getByName(args[1]).getAddress());
                        buffer.putShort(pC);
                        buffer.flip();

                        socketChannel.write(buffer);
                        selectionKey.interestOps(SelectionKey.OP_READ);
                        buffer.clear();
                        salir = 1;
                     } else {

                        socketChannel = (DatagramChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1500);

                        buffer.put(InetAddress.getByName(args[1]).getAddress());
                        buffer.putShort(pC);
                        buffer.putInt(numeroSecuencia);
                        tiempoEnviado = System.currentTimeMillis();

                        buffer.putInt((int) tiempoEnviado);
                        buffer.put(mapa.get(numeroSecuencia));
                        buffer.flip();

                        socketChannel.write(buffer);
                        selectionKey.interestOps(SelectionKey.OP_READ);
                        buffer.clear();
                     }

                  }

                  iterator.remove();
                  if (salir == 1)
                     break;
               }
            } else {
               socketChannel.register(selector, SelectionKey.OP_WRITE);
            }
            if (salir == 1)
               break;
         }
      }
   }
}
