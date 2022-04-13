
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.io.*;

public class ro2021recv {
   public static void main(String[] args) throws IOException {

      if (args.length != 2) {
         System.out.println("La sintaxis correcta es: output_file listen_port");
      } else {

         byte ipOrigenBytes[] = null;
         SocketAddress remoteAdd = null;
         byte puertoA[] = null;
         int sequenceNumber = 0;
         Integer numberRecibido = 0;

         DatagramChannel server = DatagramChannel.open();
         InetSocketAddress address = new InetSocketAddress(Short.parseShort(args[1]));
         server.bind(address);


         ByteBuffer buffer = ByteBuffer.allocate(1500);

         try {
            DataOutputStream salida = new DataOutputStream(new FileOutputStream(args[0]));

            while (true) {

               remoteAdd = server.receive(buffer);

               String ipEm = remoteAdd.toString().substring(1, remoteAdd.toString().indexOf(":"));
               Integer puertoEm = Integer.parseInt(remoteAdd.toString().substring(remoteAdd.toString().indexOf(":") + 1,remoteAdd.toString().length()));

               buffer.flip();

               if (buffer.limit() <= 10)break;

               int limits = buffer.limit();
               byte bytes[] = new byte[limits];
               buffer.get(bytes, 0, limits);
               byte data[] = new byte[limits - 14];
               data = Arrays.copyOfRange(bytes, 14, limits);

               ipOrigenBytes = new byte[4];
               ipOrigenBytes = Arrays.copyOfRange(bytes, 0, 4);
               puertoA = Arrays.copyOfRange(bytes, 4, 6);

               numberRecibido = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 6, 10)).order(ByteOrder.BIG_ENDIAN).getInt();

               long tiempo = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 10, 14)).order(ByteOrder.BIG_ENDIAN).getInt();

               buffer.clear();

               if (numberRecibido == sequenceNumber) {
                  sequenceNumber++;
                  salida.write(data);
               }

               ByteBuffer buffer2 = ByteBuffer.allocate(1700);
               buffer2.put(ipOrigenBytes);
               buffer2.put(puertoA);
               buffer2.putInt(sequenceNumber);
               buffer2.putInt((int) tiempo);
               buffer2.flip();
               InetSocketAddress serverAddress = new InetSocketAddress(ipEm, puertoEm);
               server.send(buffer2, serverAddress);

               buffer.clear();

            }
            salida.close();
         } catch (Exception e) {
         }
         server.close();
      }
   }
}