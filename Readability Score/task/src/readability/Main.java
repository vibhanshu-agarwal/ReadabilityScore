package readability;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        if(args.length == 0) {
            System.out.println("No file name provided");
            return;
        }
        //Read file passed in the argument
        String fileName = args[0];
        //Get current directory
        String currentDir = System.getProperty("user.dir");
        //Get the file path
        Path filePath = Path.of(currentDir, fileName);
        String readabilityText = Files.readString(filePath);
        //Need to find the number of sentences using regex
        String regex = "[^.!?]+[.!?]?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(readabilityText);

        List<String> sentences = new ArrayList<>();
        while (matcher.find()) {
            sentences.add(matcher.group());
        }

        AtomicInteger countChars = new AtomicInteger();
        AtomicInteger countSyllables = new AtomicInteger();
        AtomicInteger countPolysyllables = new AtomicInteger();
        //Count the number of words for each sentence
        double totalWords = sentences.stream().mapToInt(sentence -> {
            StringTokenizer stringTokenizer = new StringTokenizer(sentence);
            //For each word, get the number of characters
            int countWords = stringTokenizer.countTokens();
            while (stringTokenizer.hasMoreTokens()) {
                String word = stringTokenizer.nextToken();
                countChars.addAndGet(word.length());
                //Get the syllables in a word
                int syllables = getSyllables(word);
                //Get the polysyllables in a word
                if(syllables > 2) {
                    countPolysyllables.incrementAndGet();
                }
                countSyllables.addAndGet(syllables);
            }
            return countWords;
        }).sum();

        System.out.println("The text is:");
        System.out.println(readabilityText);
        System.out.println("Words: " + (int) totalWords);
        System.out.println("Sentences: " + sentences.size());
        System.out.println("Characters: " + countChars);
        System.out.println("Syllables: " + countSyllables);
        System.out.println("Polysyllables: " + countPolysyllables);

        //Calculate the score for each of the 4 methods
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the score you want to calculate (ARI, FK, SMOG, CL, all): ");
        final String score = scanner.nextLine();
        calculateScore(score, sentences, countChars, countSyllables, countPolysyllables, totalWords);
    }

    private static void calculateScore(String score, List<String> sentences, AtomicInteger countChars, AtomicInteger countSyllables, AtomicInteger countPolysyllables, double totalWords) {
       double averageAge = 0;
        switch (score) {
            case "ARI" -> averageAge = calculateAgeFromARI(sentences, countChars, totalWords);
            case "FK" -> averageAge = calculateAgeFromFK(sentences, countSyllables, totalWords);
            case "SMOG" -> averageAge = calculateAgeFromSMOG(countPolysyllables, sentences.size());
            case "CL" -> averageAge = calculateAgeFromCL(countChars, totalWords, sentences.size());
            case "all" -> {
                averageAge += calculateAgeFromARI(sentences, countChars, totalWords);
                averageAge += calculateAgeFromFK(sentences, countSyllables, totalWords);
                averageAge += calculateAgeFromSMOG(countPolysyllables, sentences.size());
                averageAge += calculateAgeFromCL(countChars, totalWords, sentences.size());
                averageAge /= 4;
            }
            default -> System.out.println("Invalid score");
        }
        //For each sentence, find the number of words
        System.out.println("This text should be understood in average by " + averageAge + " year olds.");
    }

    private static int calculateAgeFromCL(AtomicInteger countChars, double totalWords, int sentences) {
        double clReadabilityScore = getCLReadabilityScore(countChars.get(), totalWords, sentences);
        System.out.printf("Coleman–Liau index: %f (about %s-year-olds).", clReadabilityScore, getAge(clReadabilityScore));
        System.out.println();
        return Integer.parseInt(getAge(clReadabilityScore));
    }

    private static int calculateAgeFromSMOG(AtomicInteger countPolysyllables, int sentences) {
        double smogReadabilityScore = getSMOGReadabilityScore(countPolysyllables.get(), sentences);
        System.out.printf("Simple Measure of Gobbledygook: %f (about %s-year-olds).", smogReadabilityScore, getAge(smogReadabilityScore));
        System.out.println();
        return Integer.parseInt(getAge(smogReadabilityScore));
    }

    private static int calculateAgeFromFK(List<String> sentences, AtomicInteger countSyllables, double totalWords) {
        double fkReadabilityScore = getFKReadabilityScore(countSyllables.get(), totalWords, sentences.size());
        System.out.printf("Flesch–Kincaid readability tests: %f (about %s-year-olds).", fkReadabilityScore, getAge(fkReadabilityScore));
        System.out.println();
        return Integer.parseInt(getAge(fkReadabilityScore));
    }

    private static int calculateAgeFromARI(List<String> sentences, AtomicInteger countChars, double totalWords) {
        double ariReadabilityScore = getARIReadabilityScore(countChars.get(), totalWords, sentences.size());
        System.out.printf("Automated Readability Index: %f (about %s-year-olds).", ariReadabilityScore, getAge(ariReadabilityScore));
        System.out.println();
        return Integer.parseInt(getAge(ariReadabilityScore));
    }

    private static int getSyllables(String word) {
        //To count the number of syllables you should use letters a, e, i, o, u, y as vowels. In the short article on Vowels on Wikipedia you can see examples and intricacies with determining vowels in a word with 100% accuracy. So, let's use the following 4 rules:

        //1. Count the number of vowels in the word.
        //2. Do not count double-vowels (for example, "rain" has 2 vowels but only 1 syllable).
        //3. If the last letter in the word is 'e' do not count it as a vowel (for example, "side" has 1 syllable).
        //4. If at the end it turns out that the word contains 0 vowels, then consider this word as a 1-syllable one.
        int count = 0;
        boolean isVowel = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (isVowel(c)) {
                if (!isVowel) {
                    count++;
                }
                isVowel = true;
            } else {
                isVowel = false;
            }
        }
        if (word.endsWith("e")) {
            count--;
        }
        if (count == 0) {
            count = 1;
        }
        return count;
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y';
    }

    private static double getARIReadabilityScore(int characters, double words, int sentences) {
        //Set the scale to 2 decimal places
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.DOWN);
        return Double.parseDouble(
                df.format(
                        4.71 * (characters / words) + 0.5 * (words / sentences) - 21.43)
        );

    }

    private static double getFKReadabilityScore(int syllables, double words, int sentences) {
        //Set the scale to 2 decimal places
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.DOWN);
        return Double.parseDouble(
                df.format(
                        0.39 * (words / sentences) + 11.8 * (syllables / words) - 15.59)
        );

    }

    private static double getSMOGReadabilityScore(int polySyllables, int sentences) {
        //Set the scale to 2 decimal places
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.DOWN);
        return Double.parseDouble(
                df.format(
                        1.043 * Math.sqrt(polySyllables * (30 / sentences)) + 3.1291)
        );

    }

    private static double getCLReadabilityScore(int chars, double totalWords, int sentences) {
        //Set the scale to 2 decimal places
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.DOWN);
        return Double.parseDouble(
                df.format(
                        0.0588 * (chars / totalWords * 100) - 0.296 * (sentences / totalWords * 100) - 15.8)
        );

    }

    private static String getAge(double readabilityScore) {
        return switch ((int)readabilityScore) {
            case 1 -> "6";
            case 2 -> "7";
            case 3 -> "8";
            case 4 -> "9";
            case 5 -> "10";
            case 6 -> "11";
            case 7 -> "12";
            case 8 -> "13";
            case 9 -> "14";
            case 10 -> "15";
            case 11 -> "16";
            case 12 -> "17";
            case 13 -> "18";
            case 14 -> "22";
            default -> "22+";
        };
    }
}

