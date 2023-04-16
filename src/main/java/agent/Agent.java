package agent;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static agent.Message.MESSAGE_TYPE.*;

public class Agent {

    private String id;
    private String name;
    private String address;
    private int port;
    private List<Agent> peers;

    private ServerSocket serverSocket;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);

    private boolean listening = true;

    private BlockChain blockChain;

    private final ThreadFactory factory = new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "sync-" + cnt.getAndIncrement());
        }
    };

    // for jackson
    public Agent() {
        id = UUID.randomUUID().toString();
    }

    Agent(final String name, final String address, final int port, final Block root, final List<Agent> agents) {
        id = UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.port = port;
        this.peers = agents;
        blockChain = new BlockChain(root);
    }


    Block createBlock() {
        if (blockChain.isEmpty()) {
            return null;
        }

        Block previousBlock = getLatestBlock();
        if (previousBlock == null) {
            return null;
        }

        final int index = previousBlock.getIndex() + 1;
        final Block block = new Block(index, previousBlock.getHash(), name);
        System.out.println(String.format("%s created new block %s", name, block.toString()));
        broadcast(INFO_NEW_BLOCK, block);
        addBlock(block);
        return block;
    }

    void addBlock(Block block) {
        if (isBlockValid(block)) {
            blockChain.add(block);
        }
    }

    void startHost() {
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println(String.format("Server %s started", serverSocket.getLocalPort()));
                listening = true;
                while (listening) {
                    final AgentServerThread thread = new AgentServerThread(Agent.this, serverSocket.accept());
                    thread.start();
                }
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Could not listen to port " + port);
            }
        });
        broadcast(REQ_ALL_BLOCKS, null);
    }

    void stopHost() {
        listening = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startMine() {
        executor.execute(() -> {
            try {
                while(true) {
                    Block block = createBlock();

                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    @JsonIgnore
    public List<Block> getBlocks() {
        return blockChain.getBlocks();
    }

    private Block getLatestBlock() {
        if (blockChain.isEmpty()) {
            return null;
        }
        return blockChain.getLatestBlock();
    }

    private boolean isBlockValid(final Block block) {
        final Block latestBlock = getLatestBlock();
        if (latestBlock == null) {
            return false;
        }
        final int expected = latestBlock.getIndex() + 1;
        if (block.getIndex() != expected) {
            System.out.println(String.format("Invalid index. Expected: %s Actual: %s", expected, block.getIndex()));
            return false;
        }
        if (!Objects.equals(block.getPreviousHash(), latestBlock.getHash())) {
            System.out.println("Unmatched hash code");
            return false;
        }
        return true;
    }

    private void broadcast(Message.MESSAGE_TYPE type, final Block block) {
        peers.forEach(peer -> sendMessage(peer, type, peer.getAddress(), peer.getPort(), block));
    }

    private void sendMessage(Agent agent, Message.MESSAGE_TYPE type, String host, int port, Block... blocks) {
        if (agent.getId().equals(this.id)) {
            return;
        }
        try (

                final Socket peer = new Socket(host, port);
                final ObjectOutputStream out = new ObjectOutputStream(peer.getOutputStream());
                final ObjectInputStream in = new ObjectInputStream(peer.getInputStream())) {
                    Object fromPeer;
                    while ((fromPeer = in.readObject()) != null) {
                        if (fromPeer instanceof Message) {
                            final Message msg = (Message) fromPeer;
                            System.out.println(String.format("%d received: %s", this.port, msg.toString()));
                            if (READY == msg.type) {
                                out.writeObject(new Message.MessageBuilder()
                                        .withType(type)
                                        .withReceiver(port)
                                        .withSender(this.port)
                                        .withBlocks(Arrays.asList(blocks)).build());
                            } else if (RSP_ALL_BLOCKS == msg.type) {
                                if (!msg.blocks.isEmpty() && this.blockChain.size() == 1) {
                                    blockChain.add(msg.blocks);
                                }
                                break;
                            }
                        }
                    }
        } catch (UnknownHostException e) {
            System.err.println(String.format("Unknown host %s %d", host, port));
        } catch (IOException e) {
            System.err.println(String.format("%s couldn't get I/O for the connection to %s. Retrying...%n", getPort(), port));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }




    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
