import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ArchiverAndUnarchiver {

    /**
     * Packer & UnPacker
     *
     * @param args user input
     * @throws IOException ioError
     */
    public static void main(String[] args) throws IOException {

        String packInputFile = "test.txt";
        String packOutputFile = "test.txt.par";
        String unPackOutputFile = "test.uar";

        // Check if no arguments were passed in
        if (args.length == 0) {
            System.out.println("Sorry, no Program arguments added, trying to compress \"test.txt\" to archive \"test.par\"");
            packIt(packInputFile, packOutputFile);
            System.exit(0);
        }

        // first arg is -a
        if (args[0].equals("-a")) {
            aDecision(args, packOutputFile);
        }
        // first arg is -u
        else if (args[0].equals("-u")) {
            uDecision(args, unPackOutputFile);
        }
        // first arg is filename
        else {
            // first arg is source
            if (!extFromFullName(args[0]).equals("par")) {
                packDecision(args, packOutputFile);
            }
            // first arg is archive
            else {
                unPackDecision(args, unPackOutputFile);
            }
        }
    }

    /** -u flag decision three
     * @param args file names
     * @param unPackOutputFile unPackOutputFile
     */
    private static void uDecision(String[] args, String unPackOutputFile) {
        if (args.length == 1 || args.length > 3) {
            System.out.println("Arguments is: " + Arrays.toString(args));
            System.out.println("Something wrong with them. Compression is impossible.");
            System.exit(1);
        }
        String unPackInputFile = args[1];
        if (args.length == 3) {
            unPackOutputFile = args[2];
        }
        if (args.length == 2) {
            unPackOutputFile = nameFromFullName(args[1]) + ".uar";
        }

        unPackIt(unPackInputFile, unPackOutputFile);
    }

    /** -a flag decision three
     * @param args file names
     * @param packOutputFile packOutputFile
     * @throws IOException error
     */
    private static void aDecision(String[] args, String packOutputFile) throws IOException {
        if (args.length == 1 || args.length > 3) {
            System.out.println("Arguments is: " + Arrays.toString(args));
            System.out.println("Something wrong with them. Compression is impossible.");
            System.exit(1);
        }
        String packInputFile = args[1];
        if (args.length == 3) {
            packOutputFile = args[2];
        }
        if (args.length == 2) {
            packOutputFile = args[1] + ".par";
        }

        packIt(packInputFile, packOutputFile);
    }

    /** unpack decision three
     * @param args file names
     * @param unPackOutputFile unPackOutputFile
     */
    private static void unPackDecision(String[] args, String unPackOutputFile) {
        String unPackInputFile = args[0];
        if (args.length == 2) {
            unPackOutputFile = args[1];
        }
        if (args.length == 1) {
            unPackOutputFile = nameFromFullName(args[0]) + ".uar";
        }
        if (args.length > 2) {
            System.out.println("Arguments is: " + Arrays.toString(args));
            System.out.println("Something wrong with them. Compression is impossible.");
            System.exit(1);
        }
        unPackIt(unPackInputFile, unPackOutputFile);
    }

    /** pack decision three
     * @param args file names
     * @param packOutputFile packOutputFile
     * @throws IOException error
     */
    private static void packDecision(String[] args, String packOutputFile) throws IOException {
        String packInputFile = args[0];
        if (args.length == 2) {
            packOutputFile = args[1];
        }
        if (args.length == 1) {
            packOutputFile = args[0] + ".par";
        }
        if (args.length > 2) {
            System.out.println("Arguments is: " + Arrays.toString(args));
            System.out.println("Something wrong with them. Compression is impossible.");
            System.exit(1);
        }
        packIt(packInputFile, packOutputFile);
    }

    /**
     * Packer
     *
     * @param packInputFile  read from
     * @param packOutputFile write to
     * @throws IOException ioErrors
     */
    private static void packIt(String packInputFile, String packOutputFile) throws IOException {

        long startTime = System.nanoTime();


        // Contains all unique bytes that are on the source file
        ArrayList<Byte> uniqueBytes = new ArrayList<>();

        byte[] buffer = new byte[0];

        long source = 1;
        try (FileInputStream fin = new FileInputStream(packInputFile)) {

            source = fin.available();
            System.out.printf("SOURCE File size: %d bytes \n", source);


            buffer = new byte[fin.available()];
            // read from file to buffer
            if (fin.read(buffer, 0, buffer.length) == -1) {
                System.out.println("File \"" + packInputFile + "\" read error. Compression is impossible.");
                System.exit(1);
            }

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.out.println("File \"" + packInputFile + "\" read error. Compression is impossible.");
            System.exit(1);
        }

        for (byte b : buffer) {
            if (!uniqueBytes.contains(b))
                uniqueBytes.add(b);
        }

        System.out.println("Now compressing the file: " + packInputFile);
        System.out.println("Unique bytes: " + uniqueBytes.size());


        int keyLength = getKeyLength(uniqueBytes);


        System.out.println("It means key table will have " + keyLength + " bits.");

        Map<Byte, String> keyTable = new HashMap<>();


        // TABLE SIZE array to add to file
        ByteBuffer reserveFourBytes = ByteBuffer.allocate(4);
        reserveFourBytes.putInt(uniqueBytes.size());
        byte[] tableSize = reserveFourBytes.array();

        // TABLE array to add to file
        byte[] table = new byte[uniqueBytes.size()];

        for (int i = 0; i < uniqueBytes.size(); i++) {
            table[i] = uniqueBytes.get(i);
            String iKey = Integer.toBinaryString(i);
            // 7-bit Integer
            String key7 = String.format("%7s", iKey).replaceAll(" ", "0");

            String keyString;
            if (keyLength < 7) keyString = key7.substring(7 - keyLength);
            else keyString = key7;

            keyTable.put(uniqueBytes.get(i), keyString);
        }

        // StringBuilder added by IntelliJ advice and increased speed HERE at x5
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < buffer.length; i++) {
            byte b = buffer[i];
            String s = keyTable.get(b);
            // StringBuilder added by IntelliJ advice and increased speed HERE at x5
            out.append(s);
            if (i % 10000 == 0) System.out.println("... track pack " + (double) i / buffer.length * 100 + "%");
        }

        // DATA SIZE array to add to file
        long lengthInBits = out.length();
        ByteBuffer reserveEightBytes = ByteBuffer.allocate(8);
        reserveEightBytes.putLong(lengthInBits);
        byte[] dataSize = reserveEightBytes.array();

        // StringBuilder added by IntelliJ advice
        while (out.length() % 8 != 0) out.append("0");

        byte[] bufferOut = new byte[(out.length() / 8)];

        for (int i = 0; i < out.length(); i += 8) {
            String s = out.substring(i, i + 8);
            int intByte = Integer.parseInt(s, 2);
            bufferOut[i / 8] = (byte) intByte;
            if (i % 10000 == 0) System.out.println("... track write packed " + (double) i / out.length() * 100 + "%");
        }

        // combine final byte array
        ByteArrayOutputStream temporaryStream = new ByteArrayOutputStream();
        temporaryStream.write(tableSize);
        temporaryStream.write(dataSize);
        temporaryStream.write(table);
        temporaryStream.write(bufferOut);
        byte[] bufferFinal = temporaryStream.toByteArray();

        try (FileOutputStream fos = new FileOutputStream(packOutputFile)) {

            // write from buffer to file
            fos.write(bufferFinal, 0, bufferFinal.length);
        } catch (IOException ex) {

            System.out.println(ex.getMessage());
        }

        long arch = 1;
        try (FileInputStream ft = new FileInputStream(packOutputFile)) {

            arch = ft.available();
            System.out.printf("PACKED File size: %d bytes \n", source);

        } catch (IOException ex) {

            System.out.println(ex.getMessage());
        }


        System.out.println("Compressed to source ratio is: " + 100 * arch / source + " %");
        System.out.println("Elapsed time for pack " + (System.nanoTime() - startTime) / 1000000000.0 + " seconds.");

    }

    /**
     * UnPacker
     *
     * @param unPackInputFile  read from
     * @param unPackOutputFile write to
     */
    private static void unPackIt(String unPackInputFile, String unPackOutputFile) {

        long startTime = System.nanoTime();

        byte[] bufferIn = new byte[0];


        long arch = 1;

        try (FileInputStream fin = new FileInputStream(unPackInputFile)) {

            arch = fin.available();

            System.out.printf("PACKED File size: %d bytes \n", arch);

            bufferIn = new byte[fin.available()];
            // read from file to buffer
            if (fin.read(bufferIn, 0, bufferIn.length) == -1) {
                System.out.println("File \"" + unPackInputFile + "\" read error. Decompression is impossible.");
                System.exit(1);
            }

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.out.println("File \"" + unPackInputFile + "\" read error. Decompression is impossible.");
            System.exit(1);
        }

        // table size found
        byte[] unTableSize = Arrays.copyOfRange(bufferIn, 0, 12);
        ByteBuffer arrayAsStream = ByteBuffer.wrap(unTableSize);
        int uniqueBytesSize = arrayAsStream.getInt(0);

        // data size found
        long dataSize = arrayAsStream.getLong(4);
        System.out.println("Unpacked data size in bits: " + dataSize);

        // table found
        byte[] unTable = Arrays.copyOfRange(bufferIn, 12, 12 + uniqueBytesSize);
        // table to arraylist
        ArrayList<Byte> unUniqueBytes = new ArrayList<>();
        for (byte b : unTable) unUniqueBytes.add(b);

        int unKeyLength = getKeyLength(unUniqueBytes);

        // data found
        byte[] unBufferIn = Arrays.copyOfRange(bufferIn, 12 + uniqueBytesSize, bufferIn.length);

        // StringBuilder added by IntelliJ advice and increased speed HERE at ~50%
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < unBufferIn.length; i++) {
            byte b = unBufferIn[i];
            String s = Integer.toBinaryString((b + 256) % 256);
            String string8 = String.format("%8s", s).replaceAll(" ", "0");
            // StringBuilder added by IntelliJ advice and increased speed HERE at ~50%
            in.append(string8);
            if (i % 10000 == 0) System.out.println("... track read packed " + (double) i / unBufferIn.length * 100 + "%");
        }

        // StringBuilder added by IntelliJ advice
        while (in.length() % unKeyLength != 0) in = new StringBuilder(in.substring(0, in.length() - 1));
        byte[] bufferToFile = new byte[(in.length() / unKeyLength)];

        for (int i = 0; i < in.length(); i += unKeyLength) {
            String s = in.substring(i, i + unKeyLength);
            int intByte = Integer.parseInt(s, 2);
            bufferToFile[i / unKeyLength] = unUniqueBytes.get(intByte);
            if (i % 10000 == 0) System.out.println("... track write unpacked " + (double) i / in.length() * 100 + "%");
        }

        try (FileOutputStream fos = new FileOutputStream(unPackOutputFile)) {

            // write from buffer to file
            fos.write(bufferToFile, 0, bufferToFile.length);


        } catch (IOException ex) {

            System.out.println(ex.getMessage());
        }

        long source = 1;
        try (FileInputStream ft = new FileInputStream(unPackOutputFile)) {

            source = ft.available();
            System.out.printf("SOURCE File size: %d bytes \n", source);

        } catch (IOException ex) {

            System.out.println(ex.getMessage());
        }


        System.out.println("Compressed to source ratio is: " + 100 * arch / source + " %");
        System.out.println("Elapsed time for unpack " + (System.nanoTime() - startTime) / 1000000000.0 + " seconds");


    }

    /**
     * keyLength get from uniqueBytes source array
     *
     * @param uniqueBytes source array
     * @return keyLength
     */
    private static int getKeyLength(ArrayList<Byte> uniqueBytes) {

        if (uniqueBytes.size() > 128) {
            System.out.println("Unique bytes is more then 128. Ordinary compression is impossible.");
            System.exit(1);
        }

        int keyLength = 0;

        int[][] solution = new int[][]{{0, 2, 1}, {2, 4, 2}, {4, 8, 3}, {8, 16, 4},
                {16, 32, 5}, {32, 64, 6}, {64, 128, 7}};

        for (int[] row : solution) {
            if (uniqueBytes.size() > row[0] & uniqueBytes.size() <= row[1]) {
                keyLength = row[2];
                break;
            }
        }
        return keyLength;
    }

    private static String extFromFullName(String fullFile) {


        if (fullFile.lastIndexOf(".") > 0)

            return fullFile.substring(fullFile.lastIndexOf(".") + 1);

        else return "";
    }

    private static String nameFromFullName(String fullFile) {


        if (fullFile.lastIndexOf(".") > 0)

            return fullFile.substring(0, fullFile.lastIndexOf("."));

        else return fullFile;
    }

}
