/*retrieve synonyms off of thesaurus.com
**scans html*/

import java.util.Scanner;
import java.util.HashSet;
import java.util.HashMap;
import org.jsoup.Jsoup;
import java.util.ArrayList;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.util.Comparator;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Scanner;
import java.net.*;
import org.jsoup.HttpStatusException;

public class Synonyms {
    private static HashMap<String, Integer> words;
    private static HashMap<String, HashSet<String>> wordTypes;
    private static HashMap<String, Document> synonymDoc;
    private static File synonymDocFile;
    private static HashMap<String, HashSet<String>> savedWords;
    private static HashMap<String, HashSet<String>> savedDefWords;
    private static HashMap<String, String[]> savedDefinitions;
    private static File savedDefinitionsFile;
    private static HashSet<String> seen;
    private static int MAXNUMBER;
    private static File notValidSearchFile;
    private static String type;
    private static File savedWiki;
    private static File savedWordFile;
    private static File savedDefFile;
    private static HashSet<String> noDefinition;
    private static File noDefinitionFile;
    private static HashSet<String> description;
    private static HashSet<String> notValidSearch;
    private static Integer maxCount;
    private static HashMap<Integer, HashSet<String>> synonymRecord;
    private static HashMap<Integer, HashSet<String>> synonymDefRecord;

    /**
     * Class contructor initializes and deserializes data structures that will be used throughout program. 
     * Contructs files that serialized data structures will be stored in.
     **/
    public Synonyms() {
        type = "";
        maxCount = Integer.MIN_VALUE;
        synonymDoc = new HashMap<String, Document>();
        seen = new HashSet<String>();
        description = new HashSet<String>();
        notValidSearch = new HashSet<String>();
        noDefinition = new HashSet<String>();
        savedWordFile = new File("savedWords.txt");
        savedDefFile = new File("savedDefWords.txt");
        savedWords = new HashMap<String, HashSet<String>>();
        savedDefWords = new HashMap<String, HashSet<String>>();
        noDefinitionFile = new File("noDefinition.txt");
        savedDefinitionsFile = new File("savedDefinitionsFile");
        synonymDocFile = new File("synonymDoc.txt");
        notValidSearchFile = new File("notValidSearch");
        savedDefinitions = new HashMap<String, String[]>();
        if (notValidSearchFile.exists()) {
            notValidSearch = deserialize(notValidSearchFile);
        } 
        if (noDefinitionFile.exists()) {
            noDefinition = deserialize(noDefinitionFile);
        }
        if (savedDefinitionsFile.exists()) {
            savedDefinitions = deserialize(savedDefinitionsFile);
        } 
        if (savedWordFile.exists()) {
            savedWords = deserialize(savedWordFile);
        }
        if (savedDefFile.exists()) {
            savedDefWords = deserialize(savedDefFile);
        }
        words = new HashMap<String, Integer>();
        MAXNUMBER = 10;
    }
    /**
     * Main method of program. Still includes tester case used when experimenting with improving search accuracy.
     * Initial check for word frequency list used in program. Switch statement with cases for definition search, 
     * synonym search, and closest-match searches.
     **/
    public static void main(String[] args) throws Exception {
        Synonyms s = new Synonyms();
        savedWiki = new File("savedWiki.txt");
        if (savedWiki.exists()) {
            words = deserialize(savedWiki);
        } else {
            String url = "http://en.wiktionary.org/wiki/Wiktionary:Frequency_lists/PG/2005/08/1-10000";
            Document document = Jsoup.connect(url).get();
            Elements elements = document.select("[href^=/w][title]");
            int count = collectWords(5, 0, elements);
            for (int j= 1; j < 10; j++) {
                url = "http://en.wiktionary.org/wiki/Wiktionary:Frequency_lists/PG/2005/08/" + j + "0001-" + (j+1) + "0000";
                document = Jsoup.connect(url).get();
                elements = document.select("[href^=/w][title]");
                count = collectWords(1, count, elements);
            }
            serialize(savedWiki, words);
        }
        
        String command = args[0];
        switch (command) {
                //Add all words to dataBase from 100,000 most common words.
                case "add-words":
                    int countHowFar = 0;
                    for (String sar: words.keySet()) {
                        if (!savedWords.containsKey(sar)) { 
                            countHowFar += 1;
                            //if (countHowFar % 100 == 0) {
                                System.out.println(countHowFar + " " + sar + " " + words.get(sar));
                            //}
                            thesaurus(sar, new HashSet<String>());
                            arrayDefinition(sar);
                        }
                    }
                case "tester":
                    String ur = "http://www.dictionary.com/browse/smart";
                    Document documen = Jsoup.connect(ur).get();
                    Elements element = documen.select(".dbox-pg");
                    HashSet<String> soi = new HashSet<String>();
                    for (Element e: element) {
                        soi.add(e.text().split(" ")[0]);
                    }
                    for (String i: soi) {
                        System.out.println(i);
                    }
                    String currType = "";
                    HashMap<String, HashSet<String>> typeSynonyms= new HashMap<String, HashSet<String>>();
                    HashSet<String> currSet = new HashSet<String>();
                    String urlT = "http://www.dictionary.com/browse/smart";
                    Document document2 = Jsoup.connect(urlT).get();
                    Elements elements2 = document2.select("*");

                    for (Element e: elements2) {
                        if (e.className().equals("source-title") && e.text().contains("British Dictionary definitions for")) {
                            break;
                        }
                        if (e.className().equals("dbox-pg")) {
                            if (!currType.equals("")) {
                                typeSynonyms.put(currType, currSet);
                            }
                            currSet = new HashSet<String>();
                            currType = e.text().split(" ")[0];
                            if (typeSynonyms.containsKey(currType)) {
                                currSet = typeSynonyms.get(currType);
                            }
                        }
                        if (e.className().equals("def-number")) {
                            currSet.add(e.text().substring(0, e.text().length() -1));
                        }
                    }
                    System.out.println(typeSynonyms);
                    
                    Elements e3 = document2.select(".tail-type-synonyms");
                    int count = 0;
                    HashSet<String> set = new HashSet<String>();
                    set.add("2");
                    boolean nextIs = false;
                    Element start = e3.get(0);
                    for (Element e: start.children()){
                        for (Element e5: e.children()) {
                            String[] syns = e.text().split("\\. ");
                            for (String si: syns) {
                                if (nextIs) {
                                    System.out.println(si);
                                }
                                if (set.contains(si)) {
                                    nextIs = true;
                                } else {
                                    nextIs = false;
                                }
                                System.out.println(si);
                            }
                            break;
                        }
                    }

                    break;
                case "synonym":
                    callSynonyms(Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "closest-match":
                    callMatch(Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "smart-synonym":
                    PriorityQueue<MatchNode> pq = intelligentSynonyms(args[0], args[0], 0, Integer.parseInt(args[1]), new 
                                                            PriorityQueue<MatchNode>(MAXNUMBER, Collections.reverseOrder()));
                    break;
                case "definition":
                    callDefinition(Arrays.copyOfRange(args, 1, args.length));
                    break;
                default:
                    System.out.println("That was not one of the commands. Would you like to see the instructions again?");
                    break;
        } 
        serialize(notValidSearchFile, notValidSearch);   
        serialize(savedWiki, words);   
        serialize(savedWordFile, savedWords);
        serialize(savedDefinitionsFile, savedDefinitions);
        serialize(savedDefFile, savedDefWords);
        serialize(noDefinitionFile, noDefinition);
    }
    /**
     * Method calls synonym method and prints out the resulting words returned.
     **/
    public static void callSynonyms(String[] words) throws Exception {
        seen.add(words[0]);
        HashMap<String, HashMap<Integer, HashSet<String>>> defMap = new HashMap<String, HashMap<Integer, HashSet<String>>>();
        HashMap<String, HashSet<String>> directSynonyms = new HashMap<String, HashSet<String>>();
        PriorityQueue<MatchNode> pq = findSynonyms(words[0], words[0],  directSynonyms, 0, Integer.parseInt(words[1]), new PriorityQueue<MatchNode>(), defMap);
        while (pq.size() > 0) {
            System.out.println(pq.peek().word() + " " + pq.poll().val());
        }
    }
    /**
     * Method calls findMatch which returns the words that have the most synonyms in common with word.
     **/
    public static void callMatch(String[] words) throws Exception {
        HashSet<String> startWords = new HashSet<String>();
        PriorityQueue<MatchNode> pq =  new PriorityQueue<MatchNode>(MAXNUMBER);
        for (String str: words) {
            startWords.add(str);
            seen.add(str);
        }
        for (String arg: words) {
            pq = findMatch(startWords, arg, words, 0, pq);
        }
        while(pq.size() > 0) {
            System.out.println(pq.poll().word);
        }
    }
    public static void callDefinition(String[] words) throws Exception {
        PriorityQueue<MatchNode> pq = definition(new PriorityQueue<MatchNode>(), words);
        String[] wordList = new String[MAXNUMBER];
        int i = MAXNUMBER -1;
        while (pq.size() > 0) {
            MatchNode n = pq.poll();
            System.out.println(n.word() + " " + n.val());
            for (String so: n.def()) {
                System.out.print(so + " ");
            }
            System.out.println();
        }
    }
    public static int collectWords(int start, int count, Elements elements) {
        for (int i = start; i < elements.size(); i++) {
            if (!words.containsKey(elements.get(i).text())) {
                if (elements.get(i).text().equals("edit")) {
                    if (elements.get(i).attr("href").charAt(2) == '/') {
                    } else {
                        words.put(elements.get(i).text(), count);
                        count += 1;
                    }
                } else {
                    words.put(elements.get(i).text(), count);
                    count += 1;
                }
            }
        }
        return count;
    }
    /**
     * Serialize the synonyms and definitions of words so that we do not have to search for them more than once.
     **/
    public static <K> void serialize(File chosenFile, K obj) {
        FileOutputStream stream;
        ObjectOutputStream objectStream;
        try {
            stream = new FileOutputStream(chosenFile); 
            objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.flush();
            objectStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static <K> K deserialize(File chosenFile) {
        FileInputStream streamIn;
        ObjectInputStream objectinputstream;
        try {
            streamIn = new FileInputStream(chosenFile);
            objectinputstream = new ObjectInputStream(streamIn);
            return (K) objectinputstream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException t) {
            t.printStackTrace();
        }
        return null;
    }
    /**
     * First user interface increases accuracy of search by asking user what particular description of the word
     * he or she is looking for
     **/
    public static HashSet<String> firstUserInterface(String word) throws Exception {
        String[] exempt = {"a", "the", "and", "or", "of", "in", "to", "his", "her", "she", "him", "them", "there", 
        "their", "they're", "is", "was", "were", "when", "then", "than", "that", "this", "from", "has", "be", "been", 
        "do", "with", "as", "like", "but", "lacking", "lack of", "without", "cannot", "can't", "not", ",", ".", ";"};
        HashSet<String> exemptSet = new HashSet<String>(Arrays.asList(exempt));
        HashMap<String, String> mapOptions = new HashMap<String, String>();
        System.out.println("which of these descriptions fits what you are looking for?");
        Document document = new Document("");
        if (synonymDoc.containsKey(word)) {
            document = synonymDoc.get(word);
        } else {
            System.out.println("user false");
            String url = "http://www.thesaurus.com/browse/" + word;
            document = Jsoup.connect(url).get();
        }
        String[] ordering = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O"};
        Elements element = document.select(".synonym-description");
        String[] descript = new String[0];
        for (int i = 0; i < element.size(); i++) {
            descript = element.get(i).text().split(" ");
            mapOptions.put(ordering[i].toLowerCase(), element.get(i).text());
            System.out.print(ordering[i] + ".) ");
            for (int j = 1; j < descript.length; j ++) {
                System.out.print(descript[j] + " ");
            }
            System.out.print("(" + descript[0] + ")");
            System.out.println();
        }
        /**This will be used when determining whether to save synonyms of a certain section **/
       
        Scanner in = new Scanner(System.in);
        String answer = mapOptions.get(in.nextLine().toLowerCase());
        descript = answer.split(" ");
        type = descript[0];
        for (int i = 1; i < descript.length; i++) {
            if (!exemptSet.contains(descript[i])) {
                description.add(descript[i]);
            }
        }

        HashSet<String> syn = new HashSet<String>();
        for (Element e: element) {
            if (e.text().equals(answer)) {
                Element parent = e.parent();
                syn =  elementsToHashSet(parent.getElementsByClass("text"));
                break;
            }
        }
        return syn;
    /**
     * Refines the user's search by only adding a synonym if it is close in definition to the desired description.
     **/
    public static HashSet<String> refinedSearch(String word, HashSet<String> description, HashMap<String, HashMap<Integer, HashSet<String>>> defMap) throws Exception {
        Document document = new Document("");
        HashSet<String> syn = new HashSet<String>();
        Element e = findBestDef(word, description, defMap);
        if (e != null) {
            for (Element el: e.parent().getElementsByClass("text")) {
                syn.add(el.text());
            }
        }
        return syn;
    }
/** Increases the accuracy of the search by asking the user what form of the word he or she is looking for a synonym for. **/
  public static HashSet<String> userInterface(PriorityQueue<MatchNode> direct, HashMap<String, HashSet<String>> directSynonyms) throws Exception {
        System.out.println(direct.size());
        String first = direct.poll().word();
        int notGood = 0;
        HashSet<String> firstSet = new HashSet<String>();
        firstSet.add(first);
        HashSet<String> secondSet = new HashSet<String>();
        HashSet<String> thirdSet = new HashSet<String>();
        HashSet<String> fourthSet = new HashSet<String>();
        HashSet<String> fifthSet = new HashSet<String>();
        String second = "";
        String third = "";
        String fourth = "";
        String fifth = "";
        HashMap<String, HashSet<String>> rightSynonyms = new HashMap<String, HashSet<String>>();
        while (direct.size() > 0) {
            MatchNode node = direct.poll();
            String chosen = node.word();
            if (directSynonyms.get(first).contains(chosen)) {
                firstSet.add(chosen);
                rightSynonyms.put("a", firstSet);
            } else if (!words.containsKey(chosen) || words.get(chosen) > 500) {
                if (second.equals("")) {
                    secondSet.add(chosen);             
                    second = chosen;
                    rightSynonyms.put("b", secondSet);
                } else if (directSynonyms.get(second).contains(chosen)) {
                    System.out.println(chosen + " " + node.val());
                    secondSet.add(chosen);
                    rightSynonyms.put("b", secondSet);
                } else if ((!words.containsKey(chosen) || words.get(chosen) > 500) && third.equals("")) {
                    third = chosen;
                    thirdSet.add(chosen);
                    rightSynonyms.put("c", secondSet);
                } else if (directSynonyms.get(third).contains(chosen)) {
                    thirdSet.add(chosen);
                    rightSynonyms.put("c", thirdSet);
                } else if ((!words.containsKey(chosen) || words.get(chosen) > 500) && fourth.equals("")) {
                    fourthSet.add(chosen);
                    fourth = chosen;
                    rightSynonyms.put("d", secondSet);
                } else if (directSynonyms.get(fourth).contains(chosen)) {
                    fourthSet.add(chosen);
                    rightSynonyms.put("d", fourthSet);
                } else if ((!words.containsKey(chosen) || words.get(chosen) > 500) && fifth.equals("")) {
                    fifthSet.add(chosen);
                    fifth = chosen;
                    rightSynonyms.put("e", secondSet);
                } else if (directSynonyms.get(fifth).contains(chosen)) {
                    fifthSet.add(chosen);
                    rightSynonyms.put("e", fifthSet);
                }
            }
        }
        System.out.println("let's get a little more specific. Which of these words ");
        System.out.println("is closest to what you are looking for? (Type 'a', 'b', 'c', 'd' or 'e')");
        if (firstSet.size() > 1) {
            System.out.print("A.) " + first + " (i.e. ");
            printOptions(firstSet, first);
        } else {
            System.out.println("A.) " + first);
        }
        if (secondSet.size() > 1) {
            System.out.print("B.) " + second + " (i.e. ");
            printOptions(secondSet, second);
        } else {
            System.out.println("B.) " + second);
        }
        if (thirdSet.size() > 1) {
            System.out.print("C.) " + third + " (i.e. ");
            printOptions(thirdSet, third);   
        } else {
            System.out.println("C.) " + third);
        }
        if (fourthSet.size() > 1) {
            System.out.print("D.) " + fourth + " (i.e. ");
            printOptions(fourthSet, fourth); 
        } else {
            System.out.println("D.) " + fourth);
        }
        if (fifthSet.size() > 1) {
            System.out.print("E.) " + fifth + " (i.e. ");
            printOptions(fifthSet, fifth);  
        } else {
            System.out.println("E.) " + fifth);
        }
        Scanner in = new Scanner(System.in);
        HashSet<String> rightSynon = rightSynonyms.get(in.nextLine());
        for (String right: rightSynon) {
            seen.remove(right);
        }
        return rightSynon;
    }
    
    /** Retrieve thesaurus webpage and parse text from page. 
      * If not in "section", i.e. not an antonym, return minimum priority queue ordered by frequency weights **/
    public static PriorityQueue<MatchNode> findSynonyms(String word, String selectedWord, HashMap<String, HashSet<String>> directSynonyms,
                                                int count, int maximumCount, PriorityQueue<MatchNode> topChoices, HashMap<String, HashMap<Integer, HashSet<String>>> defMap) throws Exception{
        
        String[] exempt = {"a", "the", "and", "or", "of", "in", "to", "his", "her", "she", "him", "them", "there", 
        "their", "they're", "is", "was", "were", "when", "then", "than", "that", "this", "from", "has", "be", "been", 
        "do", "with", "as", "like", "but", "lacking", "lack of", "without", "cannot", "can't", "not", ",", ".", ";", "who"};
        HashSet<String> notIncluded = new HashSet<String>(Arrays.asList(exempt));
        if (count < maximumCount) {
            HashSet<String> syn = new HashSet<String>();
            HashSet<String> startWords = new HashSet<String>();
            if (count == 0) {
                startWords = firstUserInterface(selectedWord);
                PriorityQueue<MatchNode> direct = new PriorityQueue<MatchNode>();
                System.out.println(description);
                for (String str: description) {
                    synonymRecord = new HashMap<Integer, HashSet<String>>();
                    recordSynonyms(str, 0);
                    System.out.println(synonymRecord);
                    defMap.put(str, synonymRecord);
                }
                for (String str: startWords) {
                    syn = refinedSearch(str, new HashSet<String>(), defMap);
                    syn.add(str);
                    seen.add(selectedWord);
                    for (String sy: syn) {
                        if (words.containsKey(sy) && !seen.contains(sy)) {
                            Element e = findBestDef(sy, description, defMap);
                            if (e != null) {
                                String[] d = e.text().split(" ");
                                directSynonyms.put(sy, elementsToHashSet(e.parent().getElementsByClass("text")));
                                d = Arrays.copyOfRange(d, 1, d.length);
                                direct.add(new MatchNode(maxCount, sy, d));
                                seen.add(sy);
                            }
                        }
                    }
                }
                syn = userInterface(direct, directSynonyms);
                for (String next: syn) {
                    findSynonyms(word, next, directSynonyms, 1, maximumCount + 1, topChoices, defMap);
                }
            } else { 
                if (directSynonyms.containsKey(word)) {
                    syn = directSynonyms.get(word);
                } else {
                    syn = refinedSearch(selectedWord, description, defMap);
                }
            }
            findBestDef(selectedWord, description, defMap);
            if (topChoices.size() >= MAXNUMBER) {
                if (!selectedWord.equals(word) && topChoices.peek().val() < maxCount) {
                    topChoices.poll();
                    topChoices.add(new MatchNode(maxCount, selectedWord));
                }  
            } else {
                if (!selectedWord.equals(word)) {
                    topChoices.add(new MatchNode(maxCount, selectedWord));
                }
            }
            for (String w: syn) {
                if (!w.equals(word) && !seen.contains(w)) {
                    HashMap<String, HashMap<Integer, HashSet<String>>> defWords = new HashMap<String, HashMap<Integer, HashSet<String>>>();
                    findBestDef(w, description, defMap);
                    if (topChoices.size() >= MAXNUMBER) {
                        if (!w.equals(word) && topChoices.peek().val() < maxCount) {
                            topChoices.poll();
                            topChoices.add(new MatchNode(maxCount, w));
                        }  
                    } else {
                        if (!w.equals(word)) {
                            topChoices.add(new MatchNode(maxCount, w));
                        }
                    }
                    seen.add(w);
                    findSynonyms(word, w, directSynonyms, count + 1, maximumCount, topChoices, defMap);
                }
            }
        }
        return topChoices;
    }
    /** Method used to print out the options for the synonyms **/
    public static void printOptions(HashSet<String> set, String word) {
        int count = 0;
        for (String s: set) {
            if (!s.equals(word)) {
                if (count > 2) {
                    break;
                } 
                count +=1;
                if (count > 2 || count >= set.size() -1) {
                    System.out.print(s);
                } else {
                    System.out.print(s + ", ");
                }
            }
        }
        System.out.println(")");
    }
    /** Retrieves the synonyms for the word from thesaurus.com **/
    public static HashSet<String> thesaurus(String selectedWord, HashSet<String> syn) throws Exception {
        try {
            if (savedWords.containsKey(selectedWord)) {
                syn = savedWords.get(selectedWord);
            } else {
                System.out.println("thesaurus false");
                HashSet<String> a = new HashSet<String>();
                String url = "http://www.thesaurus.com/browse/" + selectedWord;
                Document document = Jsoup.connect(url).get();
                Elements elements = document.select("#synonyms-0 .text");
                HashSet<String> wordList = elementsToHashSet(elements);
                Elements question = document.select("section a .text");
                a = elementsToHashSet(question);    
                for (String w: wordList) {
                    if (!a.contains(w)) {
                        syn.add(w);
                    }
                }
                synonymDoc.put(selectedWord, document);
                savedWords.put(selectedWord, syn);  
            }
        } catch (org.jsoup.HttpStatusException e) {
            System.out.println("gaahhh");
            notValidSearch.add(selectedWord);
        }
        return syn;
    }
    /** 
     * Go in depth of 2 looking for word with most of selectedWords as synonyms. Start by searching selectedWords.
     * If find none, call findSynonyms on selectedWords, returning the highest weighted terms. from the lists, 
     * with depth of 2. A depth of 3 starts to move too far away from the words themselves.
     **/
    public static PriorityQueue<MatchNode> findMatch(HashSet<String> startWords, String selectedWord, 
                                            String[] selectedWords, int count, PriorityQueue<MatchNode> topChoices) throws Exception{
        if (count < 2) {
            int wordCount = 0;
            HashSet<String> syn = new HashSet<String>();
            if (savedWords.containsKey(selectedWord)) {
                syn = savedWords.get(selectedWord);
            }
            else {
                syn = thesaurus(selectedWord, syn); 
            }
            for (int i = 0; i < selectedWords.length; i++) {
                if (syn.contains(selectedWords[i])) {
                    wordCount +=1;
                }
            }
            if (wordCount > 1) {
                System.out.println("Error at: " + selectedWord);
            }
            if (topChoices.size() >= MAXNUMBER) {
                if (!startWords.contains(selectedWord) && topChoices.peek().val() < wordCount) {
                    topChoices.poll();
                    topChoices.add(new MatchNode(wordCount, selectedWord));
                }  
            } else {
                if (!startWords.contains(selectedWord)) {
                    topChoices.add(new MatchNode(wordCount, selectedWord));
                }
            }
            for (String w: syn) {
                if (!startWords.contains(w) && !seen.contains(w)) {
                    seen.add(w);
                    findMatch(startWords, w, selectedWords, count +1, topChoices);
                }
            }
        } else {
            seen.remove(selectedWord);
        }
        return topChoices;
    }
    /** Converts JSOUP elemets to a hashset **/
    public static HashSet<String> elementsToHashSet(Elements elements) {
        HashSet<String> wordList = new HashSet<String>();
        for (int i = 0; i < elements.size(); i ++) {
            wordList.add(elements.get(i).text());
        }
        return wordList;
    }
    /** Method designed to return synonyms considered to be of a difficult level. **/
    public static PriorityQueue<MatchNode> intelligentSynonyms(String word, String selectedWord, 
                                                int count, int maxCount, PriorityQueue<MatchNode> topChoices) throws Exception {
        if (count < maxCount) {
            HashSet<String> commonWords = new HashSet<String>();
            HashSet<String> wordList = new HashSet<String>();
            HashSet<String> a = new HashSet<String>();
            HashSet<String> syn = new HashSet<String>();
            if (savedWords.containsKey(selectedWord)) { 
                syn = savedWords.get(selectedWord);
            }
            else {
                String url = "http://www.thesaurus.com/browse/" + selectedWord;
                Document document = Jsoup.connect(url).get();
                Elements elements = document.select("a .text");
                wordList = elementsToHashSet(elements);
                Elements commonWord = document.select(".common-word .text");
                commonWords = elementsToHashSet(commonWord);
                Elements question = document.select("section a .text");
                a = elementsToHashSet(question);    
                for (String w: wordList) {
                    if (!a.contains(w)) {
                        syn.add(w);
                    }
                }
                synonymDoc.put(selectedWord, document);
                savedWords.put(selectedWord, syn);   
            }
            for (String w: syn) {
                if (!w.equals(word) && (savedWords.containsKey(selectedWord) || !a.contains(w))) {
                    if (!seen.contains(w)) {
                        if (topChoices.size() >= MAXNUMBER) {
                            //Length of word increases its weight
                            Integer wordNumber = words.get(w); 
                            Integer lowestNumber = words.get(topChoices.peek().word());
                            if (wordNumber == null) {
                                wordNumber = 100001;
                            }
                            if (lowestNumber == null) {
                                lowestNumber = 100001;
                            }
                            wordNumber += (10000 * w.length());
                            lowestNumber += (10000 * topChoices.peek().word().length());
                            if (commonWords.contains(w)) {
                                wordNumber = 0;
                            }
                            if (commonWords.contains(topChoices.peek().word().length())) {
                                lowestNumber = 0;
                            }
                            if (!w.equals(word) && lowestNumber < wordNumber) {
                                topChoices.poll();
                                topChoices.add(new MatchNode(words.get(w), w));
                            }  
                        } else {
                            if (!w.equals(word)) {
                                topChoices.add(new MatchNode(words.get(w), w));
                            }
                        }
                        seen.add(w);
                        intelligentSynonyms(word, w, count +1, maxCount, topChoices);
                    }
                }
            }
        } 
        return topChoices;
    }
    public static void findCase() {
        String url = "http://dictionary.reference.com/browse/" + word;
        HashMap<String, String> caseMap = new HashMap<String, String>(); 
        String[] letter = {"A", "B", "C", "D"};
        String[] wordCase = {"noun", "adjective", "verb", "adverb"};
        System.out.println();
        System.out.println("Okay, let's get started. Are you looking for a:");
        for (int i = 0; i < letter.length; i++) {
            System.out.println(letter[i] + ".) " + wordCase[i]);
            caseMap.put(letter[i].toLowerCase(), wordCase[i]);
        }
        Scanner in = new Scanner(System.in);
        String answer = caseMap.get(in.nextLine());
        if (answer.equals("adjective")) {
            type = "adj";
        } else if (answer.equals("adverb")) {
            type = "adv";
        } else {
            type = answer;
        }
    }
    /** get definition of word. Check how similar definition is to "def". Find synonyms. **/ 
    public static PriorityQueue<MatchNode> definition(PriorityQueue<MatchNode> topChoices, String[] def) throws Exception {
        String[] exempt = {"a", "the", "and", "or", "of", "in", "to", "his", "her", "she", "him", "them", "there", 
        "their", "they're", "is", "was", "were", "when", "then", "than", "that", "this", "from", "has", "be", "been", 
        "do", "with", "as", "like", "but", "lacking", "lack of", "without", "cannot", "can't", "not", ",", ".", ";"};
        HashSet<String> notIncluded = new HashSet<String>(Arrays.asList(exempt));
        HashSet<String> definition = new HashSet<String>();
        Boolean isAntonym = false;
        HashMap<String, HashMap<Integer, HashSet<String>>> defWords = new HashMap<String, HashMap<Integer, HashSet<String>>>();
        for (String s: def) {
            if (!notIncluded.contains(s)) {
                definition.add(s);
                synonymRecord =  new HashMap<Integer, HashSet<String>>();
                recordSynonyms(s, 0);
                defWords.put(s, synonymRecord);
            } 
        }
        findCase();
        HashMap<String, HashSet<String>> directSynonyms = new HashMap<String, HashSet<String>>();
        PriorityQueue<MatchNode> direct = new PriorityQueue<MatchNode>();
        HashSet<String> seen1 = new HashSet<String>();
        for (String str: definition) {
            seen.add(str);
            HashSet<String> syn = refinedSearch(str, definition, defWords);
            for (String sy: syn) {
                if (!seen1.contains(sy)) {
                    Element e = findBestDef(sy, definition, defWords);
                    if (e != null) {
                        String[] d = e.text().split(" ");
                        directSynonyms.put(sy, elementsToHashSet(e.parent().getElementsByClass("text")));
                        d = Arrays.copyOfRange(d, 1, d.length);
                        direct.add(new MatchNode(maxCount, sy, d));
                        seen1.add(sy);
                        seen.add(sy);
                    }
                }
            }
            seen1.add(str);
        }
        HashSet<String> wordsToStart = userInterface(direct, directSynonyms);
        for (String w: wordsToStart) {
            definitionFinder(isAntonym, notIncluded, defWords, w, 0, topChoices, def, directSynonyms);
        } 
        for (int i = 0; i < def.length; i++) {   ------------Will need to reference when expand on "not" and other exempt cases. 
             HashSet<String> wordList = new HashSet<String>();
             HashSet<String> a = new HashSet<String>();
             HashSet<String> syn = new HashSet<String>();
             /** Must consider antonyms of words if preceded by one of these words. **/
             if (def[i].equals(",")) {
                 isAntonym = false;
             }
             if (def[i].equals("wanting") || def[i].startsWith("lack") || def[i].equals("not") || def[i].equals("without") || def[i].equals("cannot") || def[i].equals("can't")) {
                 isAntonym = true;
                 i +=1;
                 while (notIncluded.contains(def[i])) {
                     i +=1;
                 }
                 if (i < def.length) {
                     definitionFinder(isAntonym, notIncluded, defWords, def[i], 0, topChoices, def);
                 }
             } else {
                 if (!notIncluded.contains(def[i])) {
                     definitionFinder(isAntonym, notIncluded, defWords, def[i], 0, topChoices, def);
                 }
             }
        }
        HashSet<String> exemptWords = new HashSet<String>(Arrays.asList(exempt));
        return topChoices;
    }
    /** Record the synonym options. **/
    public static void recordSynonyms(String s, int count) throws Exception {
        if (count < 2) {
            seen.add(s);
            HashSet<String> wordList = new HashSet<String>();
            HashSet<String> a = new HashSet<String>();
            HashSet<String> synonyms = new HashSet<String>();
            if (synonymRecord.containsKey(count)) {
                synonyms = synonymRecord.get(count);
            } else if (savedWords.containsKey(s)) {
                synonyms = savedWords.get(s);
            } else {
                System.out.println("false 2 " + s);
                String url = "http://www.thesaurus.com/browse/" + s;
                Document document = Jsoup.connect(url).get();
                Elements elements = document.select("#synonyms-0 .text");
                wordList = elementsToHashSet(elements);
                Elements question = document.select("section a .text");
                a = elementsToHashSet(question);
                for (String w: wordList) {
                    if (!a.contains(w)) {
                        synonyms.add(w);
                    }
                }
                synonymDoc.put(s, document);
                savedWords.put(s, synonyms);
            }
            for (String w: synonyms) {
                if (savedWords.containsKey(s) || !a.contains(w)) {
                    if (!seen.contains(w)) {
                        recordSynonyms(w, count + 1);
                    }
                }
            }
            synonymRecord.put(count, synonyms);
        }
    }
    /** Save the synonym options for a word **/
    public static void recordSynonymOptions(String s, int count) throws Exception {
        if (count < 2) {
            seen.add(s);
            HashSet<String> wordList = new HashSet<String>();
            HashSet<String> a = new HashSet<String>();
            HashSet<String> synonyms = new HashSet<String>();
            if (synonymDefRecord.containsKey(count)) {
                synonyms = synonymDefRecord.get(count);
            } else if (savedDefWords.containsKey(s)) {
                synonyms = savedDefWords.get(s);
            } else {
                String url = "http://www.dictionary.com/browse/" + s;
                Document document = Jsoup.connect(url).get();
                Elements elements = document.select(".dbox-pg");
                wordList = elementsToHashSet(elements);
                for (String w: wordList) {

                }
                synonymDoc.put(s, document);
                savedDefWords.put(s, synonyms);
            }
            for (String w: synonyms) {
                if (savedWords.containsKey(s) || !a.contains(w)) {
                    if (!seen.contains(w)) {
                        recordSynonyms(w, count + 1);
                    }
                }
            }
            synonymDefRecord.put(count, synonyms);
        }
    }
    /** Input: def is the description that we are looking for.
      * Takes in word and looks at all of the descriptions of the word. 
      * Chooses one with highest count and makes that the description. Returns that element. **/
    public static Element findBestDef(String word, HashSet<String> def, HashMap<String, HashMap<Integer, HashSet<String>>> defMap) throws Exception {
        String[] exempt = {"a", "the", "and", "or", "of", "in", "to", "his", "her", "she", "him", "them", "there", 
        "their", "they're", "is", "was", "were", "when", "then", "than", "that", "this", "from", "has", "be", "been", 
        "do", "with", "as", "like", "but", "lacking", "lack of", "without", "cannot", "can't", "not", ",", ".", ";"};
        HashSet<String> notIncluded = new HashSet<String>(Arrays.asList(exempt));
        Document document = new Document("");
        if (synonymDoc.containsKey(word)) {
            document = synonymDoc.get(word);
        } else {
            String url = "http://www.thesaurus.com/browse/" + word;
            document = Jsoup.connect(url).get();
        }
        Elements element = document.select(".synonym-description");
        String[] descript = new String[0];
        Element bestDescript = null;
        maxCount = Integer.MIN_VALUE;
        for (int i = 0; i < element.size(); i++) {
            descript = element.get(i).text().split(" ");
            if (descript[0].equals(type)) {
                int c = count(descript, defMap, notIncluded);
                if (c > maxCount) {
                    maxCount = c;
                    bestDescript = element.get(i);
                }
            }
        } 
        return bestDescript;
    }
    /** May consider the /exempt/ word relationships here as well -- when looking at definition relationship --- ie, if a word more important in the definintion, 
      * it will have more weight if found in the selectedWord definition. Also, may look for the same relationship --- if there is an of, with, because, as, etc, 
      * Then words on either side of the exempt word should be checked for similarity --- same relationship = good **/
    public static int count(String[] defWords, HashMap<String, HashMap<Integer, HashSet<String>>> definedWords, HashSet<String> exempt) throws Exception {
        int countWeight = 0;
        int numberSimilar = 0;
        for (String s: defWords) {
            if (!noDefinition.contains(s)) {
                try {
                    HashSet<String> synonyms = new HashSet<String>();
                    HashSet<String> wo = new HashSet<String>();
                    HashSet<String> ant = new HashSet<String>();
                    if (!exempt.contains(s)) {
                        /** retrieve synonyms for word in definition **/
                        synonyms = thesaurus(s, synonyms);
                        Boolean valid = false;
                        Boolean semiValid = false;
                        if (definedWords.keySet().contains(s)) {
                            countWeight += 200;
                            valid = true;
                        } else {
                            for (String key: definedWords.keySet()) {
                                /** if the word has a word in its definition which is a direct synonym
                                * of one of the defined words, add a weight of 50. **/ 
                                if (definedWords.get(key).get(0).contains(s)) {
                                    countWeight += 100;
                                    valid = true;
                                }
                                /**weight is given to word if a word in its definition has the same
                                synonyms as a word in the original definition. **/
                                for (String str: definedWords.get(key).get(0)) {
                                    if (synonyms.contains(str)) {
                                        countWeight += 1;
                                    }
                                }
                                /**if there is a word in this definition that is not even relevant
                                * to synonyms of a depth of 2, then it is not valid. Otherwise, 
                                it is semiValid. **/
                                if (definedWords.get(key).get(1).contains(s)) {
                                    semiValid = true;
                                }
                                /** if a synonym of the word in the definition is one of the defined words (words in original definition), 
                                * add weight of 50. **/
                                if (synonyms.contains(key)) {
                                    countWeight += 50;
                                    valid = true;
                                }
                            }
                        }
                        if (!valid) {
                            if (semiValid) {
                                countWeight -= 25;
                            }
                            else {
                                countWeight -= 50;
                            }
                        /**There should be extra weight if the word has multiple words that fit 
                          *into one of these categories. **/ 
                        } else {
                            numberSimilar += 1;
                        }
                    }
                } catch (HttpStatusException e) {
                    noDefinition.add(s);
                }  
            }
        }
        /**Scaling countWeight to adjust for the size of the definition. If the definition is short, then numberSimilar
         * will be smaller, but this will be adjusted for by the ratio of the original definition size to the size of the definition (will be larger).
         * If there are many words that are similar to the original definition, but the size the of this definition is much larger than the original one, 
         * then countWeight will be decreased by the ration. However, if the size of the definition is similar to the original one, then countWeight will be
         * scaled by the number of similar words with the original definition. **/
        countWeight *= Math.max(1, numberSimilar) * (definedWords.keySet().size() / (double) defWords.length);
        return countWeight;
    }
   
    public static String[] arrayDefinition(String w) throws Exception {
        String[] defWords = new String[0];
        try {
            if (savedDefinitions.containsKey(w)) {
                defWords = savedDefinitions.get(w);
            } else {
                System.out.println("false");
                String url = "http://www.dictionary.reference.com/browse/" + w;
                Document document = Jsoup.connect(url).get();
                Elements definition = document.select("div .def-content");
                if (definition.size() > 0) {
                    String defined = definition.get(0).text();
                    defWords = defined.split(" ");
                    savedDefinitions.put(w, defWords);
                }
            }
        } catch (org.jsoup.HttpStatusException e) {
            System.out.println("gahhh");
            notValidSearch.add(w);
        }
        return defWords;
    }


    /** In order to find what word in def is most important -- what word to look for with def, 
      * need to ask them. Have to complete first level for all words in def, then userInterface. **/
    public static PriorityQueue<MatchNode> definitionFinder(Boolean isAntonym, HashSet<String> exempt, HashMap<String, HashMap<Integer, HashSet<String>>> definedWords, String selectedWord, 
                                                                                        int count, PriorityQueue<MatchNode> topChoices, String[] def, HashMap<String, HashSet<String>> directSynonyms) throws Exception {
        if (count < 1) {
            HashSet<String> wordList = new HashSet<String>();
            HashSet<String> a = new HashSet<String>();
            HashSet<String> syn = new HashSet<String>();
            HashSet<String> defSet = new HashSet<String>(Arrays.asList(def));
            if (isAntonym) {
                String url = "http://www.dictionary.com/browse/" + selectedWord;
                Document document = Jsoup.connect(url).get();
                Elements question = document.select("section a #synonyms-0 .text");
                a = elementsToHashSet(question); 
                syn = a;
            } else { 
                if (directSynonyms.containsKey(selectedWord)) {
                    syn = directSynonyms.get(selectedWord);
                } else {
                    syn = refinedSearch(selectedWord, defSet, definedWords);
                }
            }
            findBestDef(selectedWord, description, definedWords);
            if (topChoices.size() >= MAXNUMBER) {
                if (!selectedWord.equals(selectedWord) && topChoices.peek().val() < maxCount) {
                    topChoices.poll();
                    topChoices.add(new MatchNode(maxCount, selectedWord));
                }  
            } else {
                if (!selectedWord.equals(selectedWord)) {
                    topChoices.add(new MatchNode(maxCount, selectedWord));
                }
            }
            for (String w: syn) {
                if (savedWords.containsKey(selectedWord) || !a.contains(w)) {
                    if (!seen.contains(w) && !definedWords.containsKey(w)) {
                        seen.add(w);
                        String[] defWords = arrayDefinition(w);
                        int countWeight = count(defWords, definedWords, exempt);
                        if (topChoices.size() >= MAXNUMBER) {
                            if (topChoices.peek().val() < countWeight && defWords != null && countWeight != 0) {
                                topChoices.poll();
                                topChoices.add(new MatchNode(countWeight, w, defWords));
                            }  
                        } else {
                            if (defWords != null) {
                                topChoices.add(new MatchNode(countWeight, w, defWords));
                            }
                        }
                        definitionFinder(isAntonym, exempt, definedWords, w, count +1, topChoices, def, directSynonyms);
                    }
                }
            }
        }
        return topChoices;
    }
    /** Create Comparable class to compare MatchNode by weight so as to sort words by corresponding frequency. **/
    private static class MatchNode implements Comparable<MatchNode>{
        private int val;
        private String word;
        private String[] def;
        public MatchNode(int value, String word) {
            val = value;
            this.word = word;
        }
        public MatchNode(int value, String word, String[] def) {
            val = value;
            this.word = word;
            this.def = def;
        }
        public void setVal(int value) {
            val = value;
        }
        public String[] def() {
            return def;
        }
        public String word() {
            return word;
        }
        public int val() {
            return val;
        }
        /* Sorts nodes by weight- word with heigher frequency is larger */
        public int compareTo(MatchNode s) {
            if (val < s.val) {
                return 1;
            } else if (val >  s.val)  {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
