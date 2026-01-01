package org.emulinker.kaillera.model.impl;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.util.EmuLang;
import org.emulinker.util.EmuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoFireScanner implements AutoFireDetector {
    protected static final Logger log = LoggerFactory.getLogger(AutoFireScanner.class);

    // MAX DELAY, MIN REPEITIONS
    private static final int[][] SENSITIVITY_TABLE = {{0, 0}, // 0 means disable autofire detect
            {2, 13}, // 1 is least sensitive
            {3, 11}, {4, 9}, {5, 7}, {6, 5} // 5 is most sensitive
    };

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    protected KailleraGame game;
    protected int sensitivity;
    protected int maxDelay;
    protected int minReps;

    protected ScanningJob[] scanningJobs;

    public AutoFireScanner(KailleraGame game, int sensitivity) {
        this.game = game;
        setSensitivity(sensitivity);
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        if (sensitivity < 0 || sensitivity > 5)
            this.sensitivity = 0;
        else {
            this.sensitivity = sensitivity;
            maxDelay = SENSITIVITY_TABLE[sensitivity][0];
            minReps = SENSITIVITY_TABLE[sensitivity][1];
        }
    }

    public void start(int numPlayers) {
        if (sensitivity <= 0)
            return;

        scanningJobs = new ScanningJob[numPlayers];
    }

    public void addPlayer(KailleraUser player, int playerNumber) {
        if (sensitivity <= 0 || scanningJobs == null)
            return;

        scanningJobs[(playerNumber - 1)] = new ScanningJob(player, playerNumber);
    }

    public void stop(int playerNumber) {
        if (sensitivity <= 0 || scanningJobs == null)
            return;

        scanningJobs[(playerNumber - 1)].stop();
    }

    public void stop() {
        if (sensitivity <= 0 || scanningJobs == null)
            return;

        for (int i = 0; i < scanningJobs.length; i++)
            scanningJobs[i].stop();
    }

    public void addData(int playerNumber, byte[] data, int bytesPerAction) {
        if (sensitivity <= 0 || scanningJobs == null)
            return;

        scanningJobs[(playerNumber - 1)].addData(data, bytesPerAction);
    }

    protected class ScanningJob implements Runnable {
        private KailleraUser user;
        private int playerNumber;

        private int bytesPerAction = -1;
        private int sizeLimit;
        private int bufferSize = 5;
        private int size = 0;

        private byte[][] buffer;
        private int head = 0;
        private int tail = 0;
        private int pos = 0;

        private boolean running = false;
        private boolean stopFlag = false;

        protected ScanningJob(KailleraUser user, int playerNumber) {
            this.user = user;
            this.playerNumber = playerNumber;

            sizeLimit = ((maxDelay + 1) * minReps * 5);
            buffer = new byte[bufferSize][sizeLimit];
        }

        protected synchronized void addData(byte[] data, int bytesPerAction) {
            if ((pos + data.length) >= sizeLimit) {
                int firstSize = (sizeLimit - pos);
                // log.debug("firstSize="+firstSize);
                System.arraycopy(data, 0, buffer[tail], pos, firstSize);
                tail = ((tail + 1) % bufferSize);
                // log.debug("tail="+tail);
                System.arraycopy(data, firstSize, buffer[tail], 0, (data.length - firstSize));
                pos = (data.length - firstSize);
                // log.debug("pos="+pos);
                size++;

                if (this.bytesPerAction <= 0)
                    this.bytesPerAction = bytesPerAction;

                if (!running)
                    EXECUTOR.submit(this);
            } else {
                System.arraycopy(data, 0, buffer[tail], pos, data.length);
                pos += data.length;
                // log.debug("pos="+pos);
            }
        }

        protected void stop() {
            this.stopFlag = true;
        }

        public void run() {
            // long st = System.currentTimeMillis();
            synchronized (this) {
                running = true;
            }

            try {
                while (size > 0 && !stopFlag) {
                    byte[] data = null;
                    synchronized (this) {
                        data = buffer[head];
                        // log.debug("Scanning " + data.length + " bytes from buffer position " +
                        // head);
                        head = ((head + 1) % bufferSize);
                        size--;
                    }

                    // determine the number of actions in this array
                    int actionCount = (data.length / bytesPerAction);
                    byte[] thisAction = new byte[bytesPerAction];
                    byte[] lastAction = new byte[bytesPerAction];
                    byte[] actionA = new byte[bytesPerAction];
                    int aPos = 0;
                    int aCount = 0;
                    int aSequence = 0;
                    int lastASequence = 0;
                    int aSequenceCount = 0;
                    byte[] actionB = new byte[bytesPerAction];
                    int bPos = 0;
                    int bCount = 0;
                    int bSequence = 0;
                    int lastBSequence = 0;
                    int bSequenceCount = 0;

                    for (int i = 0; i < actionCount; i++) {
                        System.arraycopy(data, (i * bytesPerAction), thisAction, 0, bytesPerAction);
                        // log.debug("thisAction=" + EmuUtil.bytesToHex(thisAction) + " actionA=" +
                        // EmuUtil.bytesToHex(actionA) + " aCount=" + aCount + " actionB=" +
                        // EmuUtil.bytesToHex(actionB) + " bCount=" + bCount + " aSequence=" +
                        // aSequence
                        // + " aSequenceCount=" + aSequenceCount + " bSequence=" + bSequence + "
                        // bSequenceCount=" + bSequenceCount);

                        if (aCount == 0) {
                            System.arraycopy(thisAction, 0, actionA, 0, bytesPerAction);
                            aPos = i;
                            aCount = 1;
                            aSequence = 1;
                        } else if (Arrays.equals(thisAction, actionA)) {
                            aCount++;
                            if (Arrays.equals(thisAction, lastAction))
                                aSequence++;
                            else {
                                if (lastASequence == aSequence && aSequence <= maxDelay)
                                    aSequenceCount++;
                                else
                                    aSequenceCount = 0;
                                lastASequence = aSequence;
                                aSequence = 1;
                            }
                        } else if (bCount == 0) {
                            System.arraycopy(thisAction, 0, actionB, 0, bytesPerAction);
                            bPos = i;
                            bCount = 1;
                            bSequence = 1;
                        } else if (Arrays.equals(thisAction, actionB)) {
                            bCount++;
                            if (Arrays.equals(thisAction, lastAction))
                                bSequence++;
                            else {
                                if (lastBSequence == bSequence && bSequence <= maxDelay)
                                    bSequenceCount++;
                                else
                                    bSequenceCount = 0;
                                lastBSequence = bSequence;
                                bSequence = 1;
                            }
                        } else {
                            actionA = lastAction;
                            aCount = 1;
                            aSequence = 1;
                            aSequenceCount = 0;
                            actionB = thisAction;
                            bCount = 1;
                            bSequence = 0;
                            bSequenceCount = 0;
                        }

                        System.arraycopy(thisAction, 0, lastAction, 0, bytesPerAction);

                        // if(aSequenceCount >= 3 && bSequenceCount >= 3 && !stopFlag)
                        // {
                        // log.debug("thisAction=" + EmuUtil.bytesToHex(thisAction) + " actionA=" +
                        // EmuUtil.bytesToHex(actionA) + " aCount=" + aCount + " actionB=" +
                        // EmuUtil.bytesToHex(actionB) + " bCount=" + bCount + " aSequence=" +
                        // aSequence
                        // + " aSequenceCount=" + aSequenceCount + " bSequence=" + bSequence + "
                        // bSequenceCount=" + bSequenceCount);
                        // }

                        if (aSequenceCount >= minReps && bSequenceCount >= minReps && !stopFlag) {
                            KailleraGameImpl gameImpl = (KailleraGameImpl) game;
                            gameImpl.announce(EmuLang.getString("AutoFireScanner2.AutoFireDetected", //$NON-NLS-1$
                                    user.getName()));
                            log.info("AUTOUSERDUMP\t" //$NON-NLS-1$
                                    + EmuUtil.DATE_FORMAT.format(gameImpl.getStartDate()) + "\t" //$NON-NLS-1$
                                    + (aSequence < bSequence ? aSequence : bSequence) + "\t" //$NON-NLS-1$
                                    + game.getID() + "\t" //$NON-NLS-1$
                                    + game.getRomName() + "\t" + user.getName() + "\t" //$NON-NLS-1$ //$NON-NLS-2$
                                    + user.getSocketAddress().getAddress().getHostAddress());
                            // log.debug("thisAction=" + EmuUtil.bytesToHex(thisAction) + "
                            // actionA=" +
                            // EmuUtil.bytesToHex(actionA) + " aCount=" + aCount + " actionB=" +
                            // EmuUtil.bytesToHex(actionB) + " bCount=" + bCount + " aSequence=" +
                            // aSequence
                            // + " aSequenceCount=" + aSequenceCount + " bSequence=" + bSequence + "
                            // bSequenceCount=" + bSequenceCount);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("AutoFireScanner thread for " + user + " caught exception!", e); //$NON-NLS-1$ //$NON-NLS-2$
            } finally {
                synchronized (this) {
                    running = false;
                }
            }

            // long et = (System.currentTimeMillis()-st);
            // log.debug("Scanning completed in " + et + " ms");
        }
    }
}
