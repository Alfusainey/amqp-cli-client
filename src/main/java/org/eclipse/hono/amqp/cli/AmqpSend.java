package org.eclipse.hono.amqp.cli;

import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.qpid.proton.message.Message;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonSender;

/**
 * A simple command-line client for interacting with the AMQP adapter.
 */
public class AmqpSend {

    private static final Vertx VERTX = Vertx.vertx();

    public static void main(String[] args) {
        final Options options = generateOptions();
        try {
            final CommandLine commandLine = parseCommandLineOptions(options, args);
            final boolean help = commandLine.hasOption("h");
            if (help) {
                printHelp(options);
            } else {
                final String broker = commandLine.getOptionValue("b");
                final String targetAddress = commandLine.getOptionValue("a");
                final String to = commandLine.getOptionValue("to");
                final String username = commandLine.getOptionValue("u");
                final String password = commandLine.getOptionValue("p");
                final Message message = ProtonHelper.message(commandLine.getOptionValue("m"));
                message.setAddress(to);

                final CountDownLatch sent = new CountDownLatch(0);
                connectToAdapter(broker, username, password).setHandler(result -> {
                    final PrintWriter pw = new PrintWriter(System.out);
                    if (result.succeeded()) {
                        final ProtonSender sender = result.result().createSender(targetAddress);
                        sender.openHandler(remoteAttach -> {
                            if (remoteAttach.succeeded()) {
                                sender.send(message, delivery -> {
                                    pw.println(delivery.getRemoteState());
                                    pw.flush();
                                    sent.countDown();
                                });
                            }
                        }).open();
                        
                    } else {
                        pw.println(result.cause());
                        pw.flush();
                    }
                });
                
                try {
                    sent.await();
                } catch (InterruptedException e) {
                    // TODO
                }
            }

        } catch (ParseException e) {
            final PrintWriter pw = new PrintWriter(System.out);
            pw.println(e.getMessage());
            pw.flush();
            printHelp(options);
        }
    }

    private static int getPort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    //----------------------------------< Vertx-proton >---

    private static Future<ProtonConnection> connectToAdapter(final String brokerAddress, final String username,
            final String password) {
        final Future<ProtonConnection> result = Future.future();
        final String[] hostAndPort = brokerAddress.split(":", 2);
        if (hostAndPort.length != 2) {
            result.fail("Broker address does not comply with expected pattern [<localhost>:<port>]");
        } else {
            final ProtonClientOptions options = new ProtonClientOptions();
            final ProtonClient client = ProtonClient.create(VERTX);
            final String host = hostAndPort[0];
            final int port = getPort(hostAndPort[1]);
            if (username != null && password != null) {
                // SASL PLAIN authc.
                client.connect(options, host, port, username, password, conAttempt -> {
                    if (conAttempt.failed()) {
                        result.fail(conAttempt.cause());
                    } else {
                        final ProtonConnection adapterConnection = conAttempt.result();
                        adapterConnection.openHandler(remoteOpen -> {
                            if (remoteOpen.succeeded()) {
                                result.complete(adapterConnection);
                            }
                        }).open();
                    }
                });
            } else {
                // SASL ANONYMOUS authc.
                client.connect(host, port, conAttempt -> {
                    if (conAttempt.failed()) {
                        
                    } else {
                        
                    }
                });
            }
            
        }
        return result;
    }

    //----------------------------------< Commons CLI (Definition and Parsing) >---
    /**
     * Generate the option definitions for this client.
     * @return A definition of command-line options.
     */
    private static Options generateOptions() {
        final Option brokerOption = Option.builder("b")
                .required()
                .hasArg()
                .longOpt("broker")
                .desc("url of the AMQP adapter to connect to")
                .build();
        final Option targetAddressOption = Option.builder("a")
                .required(false)
                .hasArg()
                .longOpt("address")
                .desc("The target address of the sender link (for non-anonymous links)")
                .build();
        final Option messageAddressOption = Option.builder("to")
                .required()
                .hasArg()
                .longOpt("to") // no long-rep for this option
                .desc("The message address.")
                .build();
        final Option usernameOption = Option.builder("u")
                .required(false)
                .hasArg()
                .longOpt("username")
                .desc("The username to connect as")
                .build();
        final Option passwordOption = Option.builder("p")
                .required(false)
                .hasArg()
                .longOpt("password")
                .desc("The password to connect as")
                .build();
        final Option messageOption = Option.builder("m")
                .required()
                .hasArg()
                .longOpt("message")
                .desc("The message to forward downstream")
                .build();
        final Option helpOption = Option.builder("h")
                .required(false)
                .hasArg()
                .longOpt("help")
                .desc("Print help information")
                .build();

        final Options options = new Options();

        options.addOption(brokerOption);
        options.addOption(targetAddressOption);
        options.addOption(messageAddressOption);
        options.addOption(usernameOption);
        options.addOption(passwordOption);
        options.addOption(messageOption);
        options.addOption(helpOption);

        return options;
    }
    
    /**
     * Parses the given command-line arguments.
     * 
     * @param options The command-line options to use for parsing the arguments.
     * @param arguments The command-line arguments to parse.
     * @return a command line after successful parsing.
     * @throws ParseException 
     */
    private static CommandLine parseCommandLineOptions(final Options options, final String[] arguments) throws ParseException {
        final CommandLineParser parser = new DefaultParser();

        return parser.parse(options, arguments);
    }
    
    
    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        final String cmdLineSyntax = "AmqpSend";
        final String usageHeader = "A Simple Command-line client for sending telemetry/event messages to the AMQP adapter\n\n";
        final String usageFooter = "\nSee https://www.eclipse.org/hono/user-guide//amqp-adapter for more information on how to use this client to interract with the adapter\n";
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("\n=======================================================================");
        pw.flush();
        formatter.printHelp(cmdLineSyntax, usageHeader, options, usageFooter);
    }    
}
