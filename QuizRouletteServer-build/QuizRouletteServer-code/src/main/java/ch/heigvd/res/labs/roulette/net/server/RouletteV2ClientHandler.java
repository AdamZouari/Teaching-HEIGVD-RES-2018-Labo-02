package ch.heigvd.res.labs.roulette.net.server;

import ch.heigvd.res.labs.roulette.data.EmptyStoreException;
import ch.heigvd.res.labs.roulette.data.IStudentsStore;
import ch.heigvd.res.labs.roulette.data.JsonObjectMapper;
import ch.heigvd.res.labs.roulette.data.StudentsList;
import ch.heigvd.res.labs.roulette.net.protocol.ByeCommandResponse;
import ch.heigvd.res.labs.roulette.net.protocol.InfoCommandResponse;
import ch.heigvd.res.labs.roulette.net.protocol.LoadCommandResponse;
import ch.heigvd.res.labs.roulette.net.protocol.RandomCommandResponse;
import ch.heigvd.res.labs.roulette.net.protocol.RouletteV2Protocol;
import static ch.heigvd.res.labs.roulette.net.server.RouletteV1ClientHandler.LOG;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the Roulette protocol (version 2).
 *
 * @author Olivier Liechti
 * @author Adam Zouari
 * @author Nair Alic
 */
public class RouletteV2ClientHandler implements IClientHandler {

    final static Logger LOG = Logger.getLogger(RouletteV2ClientHandler.class.getName());
    public final static String RESPONSE_SUCCESS = "success";
    private final IStudentsStore store;
    private int nbOfCommands;

    public RouletteV2ClientHandler(IStudentsStore store) {
        this.store = store;
        this.nbOfCommands = 0;
    }

    @Override
    public void handleClientConnection(InputStream is, OutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os));

        int nbOfStudentsBeforeLoad, nbOfStudentsAfterLoad;
        StudentsList studentsList;

        writer.println("Hello. Online HELP is available. Will you find it?");
        writer.flush();

        String command;
        boolean done = false;
        while (!done && ((command = reader.readLine()) != null)) {
            LOG.log(Level.INFO, "COMMAND: {0}", command);
            nbOfCommands++;
            switch (command.toUpperCase()) {
                
                case RouletteV2Protocol.CMD_RANDOM:
                    RandomCommandResponse rcResponse = new RandomCommandResponse();
                    try {
                        rcResponse.setFullname(store.pickRandomStudent().getFullname());
                    } catch (EmptyStoreException ex) {
                        rcResponse.setError("There is no student, you cannot pick a random one");
                    }
                    writer.println(JsonObjectMapper.toJson(rcResponse));
                    writer.flush();
                    break;
                    
                case RouletteV2Protocol.CMD_HELP:
                    writer.println("Commands: " + Arrays.toString(RouletteV2Protocol.SUPPORTED_COMMANDS));
                    break;
                    
                case RouletteV2Protocol.CMD_INFO:
                    InfoCommandResponse response = new InfoCommandResponse(RouletteV2Protocol.VERSION, store.getNumberOfStudents());
                    writer.println(JsonObjectMapper.toJson(response));
                    writer.flush();
                    break;

                case RouletteV2Protocol.CMD_LIST:
                    studentsList = new StudentsList();
                    studentsList.setStudents(store.listStudents());
                    writer.println(JsonObjectMapper.toJson(studentsList));
                    writer.flush();
                    break;

                case RouletteV2Protocol.CMD_CLEAR:
                    store.clear();
                    writer.println(RouletteV2Protocol.RESPONSE_CLEAR_DONE);
                    writer.flush();
                    break;

                case RouletteV2Protocol.CMD_LOAD:
                    writer.println(RouletteV2Protocol.RESPONSE_LOAD_START);
                    writer.flush();
                    nbOfStudentsBeforeLoad = store.getNumberOfStudents();
                    store.importData(reader);
                    nbOfStudentsAfterLoad = store.getNumberOfStudents();
                    writer.println(JsonObjectMapper.toJson(new LoadCommandResponse(RESPONSE_SUCCESS, nbOfStudentsAfterLoad - nbOfStudentsBeforeLoad)));
                    writer.flush();
                    break;
                    
                case RouletteV2Protocol.CMD_BYE:
                    writer.println(JsonObjectMapper.toJson(new ByeCommandResponse("success", nbOfCommands)));
                    done = true;
                    break;
                    
                default:
                    nbOfCommands--;
                    writer.println("Huh? please use HELP if you don't know what commands are available.");
                    writer.flush();
                    break;
            }
            writer.flush();
        }
    }
}
