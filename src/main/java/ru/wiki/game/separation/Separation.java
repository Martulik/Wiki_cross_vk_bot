package ru.wiki.game.separation;

import lombok.extern.slf4j.Slf4j;
import ru.wiki.game.links.AbstractLinkFetcher;
import ru.wiki.game.links.BacklinksFetcher;
import ru.wiki.game.links.ExportLinksFetcher;
import ru.wiki.game.util.DataParse;
import ru.wiki.game.util.URLFetch;
import ru.wiki.game.util.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

class ThreadedLinkFetcher implements Runnable {
    static volatile boolean isDone = false;

    private final List<String> writeTo;
    private final List<String> task;
    private final List<String> targets;
    private final ConcurrentHashMap<String, String> map;
    private final AbstractLinkFetcher linkFetcher;

    public ThreadedLinkFetcher(List<String> writeTo, List<String> task,
                               AbstractLinkFetcher linkFetcher, List<String> targets,
                               ConcurrentHashMap<String, String> map) {
        this.writeTo = writeTo;
        this.task = task;
        this.linkFetcher = linkFetcher;
        this.targets = targets;
        this.map = map;
    }

    public void run() {
        Iterator<String> it = this.task.iterator();

        while (!ThreadedLinkFetcher.isDone && it.hasNext()) {
            String link = it.next();
            ArrayList<String> linksOf;
            try {
                linksOf = linkFetcher.getLinks(link, new ArrayList<>(targets));
            } catch (IOException e) {
                continue;
            }

            for (String linkOf : linksOf) {
                this.map.putIfAbsent(linkOf.toLowerCase(), link);
                this.writeTo.add(linkOf);
            }

            List<String> common = Utilities.retainAllIgnoreCase(linksOf, this.targets);
            if (!common.isEmpty()) {
                ThreadedLinkFetcher.isDone = true;
            }
        }
    }
}

@Slf4j
public class Separation {
    private static final ExportLinksFetcher linksFetcher = new ExportLinksFetcher();
    private static final BacklinksFetcher backlinksFetcher = new BacklinksFetcher();

    private int numDegrees;
    private final Stack<String> path;
    private final Stack<String> embeddedPath;
    private boolean pathExists;

    private final String startArticle;
    private final String endArticle;

    private List<String> links;
    private List<String> backlinks;

    private final ConcurrentHashMap<String, String> predecessors;
    private final ConcurrentHashMap<String, String> successors;

    public Separation(String start, String end) throws IOException {
        this.startArticle = start;
        this.endArticle = end;

        this.numDegrees = 0;
        this.path = new Stack<>();
        this.embeddedPath = new Stack<>();
        this.pathExists = false;

        this.links = Collections.synchronizedList(new ArrayList<>());
        this.backlinks = Collections.synchronizedList(new ArrayList<>());
        this.predecessors = new ConcurrentHashMap<>();
        this.successors = new ConcurrentHashMap<>();

        this.pathExists = this.getSeparation0();
        if (this.pathExists) {
            return;
        }

        this.pathExists = this.getSeparation1();
        if (this.pathExists) {
            return;
        }

        if (this.links.isEmpty()) {
            log.info("No links exist on the starting page! Unable to complete the path.");
            return;
        }

        this.pathExists = this.getSeparation2();
        if (this.pathExists) {
            return;
        }

        if (this.backlinks.isEmpty()) {
            log.info("No backlinks exist on the ending page! Unable to complete the path.");
            return;
        }

        this.pathExists = this.getSeparation3();
    }

    public int getNumDegrees() {
        return this.numDegrees;
    }

    public Stack<String> getEmbeddedPath() {
        return this.embeddedPath;
    }

    public Stack<String> getPath() {
        return this.path;
    }

    public boolean getPathExists() {
        return this.pathExists;
    }

    public void computeEmbeddedPath() throws IOException {
        for (int i = 0; i < path.size() - 1; ++i) {
            String current = path.get(i);
            String next = path.get(i + 1);

            String url = URLFetch.getExportURL(current);
            String data = URLFetch.getData(url);

            this.embeddedPath.push(DataParse.parseEmbeddedArticle(data, next));
        }
    }

    private boolean getSeparation0() {
        log.info("Checking 0 Degrees Separation: ");

        this.path.push(this.startArticle);

        boolean equal = this.startArticle.equalsIgnoreCase(this.endArticle);
        if (equal) {
            log.info("The starting article equals the end article, 0 degrees of separation found.");
        } else {
            log.info("The starting article does not equal the end article, 0 degrees of separation not found.");
        }
        return equal;
    }

    private boolean getSeparation1() throws IOException {
        log.info("Checking 1 Degree Separation: ");

        ++(this.numDegrees);

        ArrayList<String> targets = new ArrayList<>(1);
        targets.add(this.endArticle);
        this.links = linksFetcher.getLinks(this.startArticle, targets);

        log.info("Fetched " + this.links.size() + " link(s) from the starting article.");

        boolean contain = Utilities.containsIgnoreCase(this.links, this.endArticle);
        if (contain) {
            this.path.push(this.endArticle);
            log.info("End link is contained within the links of the starting article, 1 degree of separation found.");
            this.computeEmbeddedPath();
        } else {
            log.info("End link is not contained within the links of the starting article, 1 degree of separation not found.");
        }
        return contain;
    }

    private boolean getSeparation2() throws IOException {
        log.info("Checking 2 Degrees Separation: ");

        ++(this.numDegrees);
        this.backlinks = backlinksFetcher.getLinks(this.endArticle, new ArrayList<>(this.links));

        log.info("Fetched " + this.backlinks.size() + " backlink(s) from the ending article.");

        List<String> common = Utilities.retainAllIgnoreCase(this.backlinks, this.links);
        if (common.isEmpty()) {
            log.info("Found no middle ground articles between start links and end backlinks, two degrees of separation not found.");
            return false;
        }

        String middle = common.iterator().next();
        this.path.push(middle);
        this.path.push(this.endArticle);

        log.info("Found middle ground article, \"" + middle + "\", between start links and end backlinks, 2 degrees of separation found.");

        this.computeEmbeddedPath();
        return true;
    }

    private boolean getSeparation3() throws IOException {
        ++(this.numDegrees);

        for (String s : this.links)
            predecessors.put(s.toLowerCase(),
                    this.startArticle);
        for (String s : this.backlinks)
            successors.put(s.toLowerCase(),
                    this.endArticle);

        while (true) {
            log.info("Checking for " + this.numDegrees + " Degrees Separation: ");
            log.info("Links Size: " + this.links.size());
            log.info("Backlinks Size: " + this.backlinks.size());

            if (this.links.size() <= this.backlinks.size()) {
                log.info("Fetching links of current links.");
                links = this.getSeparation3GrowGraph(linksFetcher);
                if (links.isEmpty()) {
                    return false;
                }
            } else {
                log.info("Fetching backlinks of current backlinks.");
                backlinks = this.getSeparation3GrowGraph(backlinksFetcher);
                if (backlinks.isEmpty()) {
                    return false;
                }
            }

            List<String> common = Utilities.retainAllIgnoreCase(this.links, this.backlinks);
            if (!common.isEmpty()) {
                String middle = common.iterator().next();

                log.info("Found middle ground article, \"" + middle + "\", between links and backlinks, " +
                        this.numDegrees + " degrees of separation found.");

                String currentPredecessor = middle;
                String currentSuccessor = middle;

                Stack<String> backtrace = new Stack<>();
                while (!currentPredecessor.equalsIgnoreCase(this.startArticle)) {
                    String oldPredecessor = currentPredecessor;
                    currentPredecessor = predecessors.get(
                            currentPredecessor.toLowerCase());
                    backtrace.push(currentPredecessor);

                    log.info("Found predecessor, \"" + currentPredecessor + "\", of link \"" + oldPredecessor + "\".");
                }

                backtrace.pop();
                while (!backtrace.isEmpty()) {
                    this.path.push(backtrace.pop());
                }

                while (!currentSuccessor.equalsIgnoreCase(this.endArticle)) {
                    String oldSuccessor = currentSuccessor;
                    this.path.push(currentSuccessor);
                    currentSuccessor = successors.get(
                            currentSuccessor.toLowerCase());
                    log.info("Found successor, \"" + currentSuccessor + "\", of link \"" + oldSuccessor + "\".");
                }
                this.path.push(this.endArticle);
                this.computeEmbeddedPath();
                return true;
            } else {
                log.info("Found no middle ground articles between links and backlinks, " + this.numDegrees +
                        " degrees of separation not found.");
                ++(this.numDegrees);
            }
        }
    }

    private ArrayList<String> getSeparation3GrowGraph(AbstractLinkFetcher fetcher) {
        List<String> newLinks = Collections.synchronizedList(new ArrayList<>());

        boolean isStartSide = fetcher == Separation.linksFetcher;
        List<String> thisSide = isStartSide ? this.links : this.backlinks;
        List<String> otherSide = isStartSide ? this.backlinks : this.links;
        ConcurrentHashMap<String, String> map = isStartSide ? this.predecessors : this.successors;

        final int MAX_NUM_THREADS = 128;
        final int MAX_IDEAL_PER_THREAD = 32;

        int numThreads = 1;
        int numPerThread = thisSide.size();

        if (thisSide.size() >= MAX_IDEAL_PER_THREAD) {
            numThreads = thisSide.size() / MAX_IDEAL_PER_THREAD + 1;

            if (numThreads > MAX_NUM_THREADS) {
                numThreads = MAX_NUM_THREADS;
            }

            numPerThread = thisSide.size() / numThreads;
        }

        ArrayList<Thread> threads = new ArrayList<>();
        ThreadedLinkFetcher.isDone = false;

        for (int i = 1; i <= numThreads; ++i) {
            int fromIndex = (i - 1) * numPerThread;
            int toIndex = fromIndex + numPerThread;

            if (i == numThreads) toIndex = thisSide.size();
            if (toIndex >= thisSide.size()) toIndex = thisSide.size();

            List<String> task = thisSide.subList(fromIndex, toIndex);

            Thread tgg = new Thread(new ThreadedLinkFetcher(newLinks, task, fetcher, otherSide, map));
            threads.add(tgg);
            tgg.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>(newLinks);
    }
}
