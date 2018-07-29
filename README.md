# amqp-cli-client
A simple command-line client for interacting with the Hono AMQP adapter

### Example (usage information)
The `-h` flag or simply invoking the command without any argument will print out help information to standard output.
The example below shows how to build the command and print out usage information:

```bash
mvn clean install
java -jar <jar-file-name> -h
```
```bash
=======================================================================
usage: AmqpSend
A Simple Command-line client for sending telemetry/event messages to the
AMQP adapter

 -a,--address <arg>    The target address of the sender link (for
                       non-anonymous links)
 -b,--broker <arg>     url of the AMQP adapter to connect to
 -h,--help <arg>       Print help information
 -m,--message <arg>    The message to forward downstream
 -p,--password <arg>   The password to connect as
 -to,--to <arg>        The message address.
 -u,--username <arg>   The username to connect as

See https://www.eclipse.org/hono/user-guide//amqp-adapter for more
information on how to use this client to interract with the adapter
```

The AMQP client outputs the `delivery state` of the remote peer to standard output. For instance, if we try to use the client
to publish a (event/telemetry) message with an `invalid` message address, the following output is displayed to standard output:

`Rejected{error=Error{condition=hono:bad-request, description='Invalid address for unauthenticated devices', info=null}}`
