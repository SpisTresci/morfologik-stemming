package morfologik.stemming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import morfologik.fsa.FSA;
import morfologik.fsa.FSABuilder;
import morfologik.fsa.FSAUtils;

import org.junit.Test;

/*
 *
 */
public class DictionaryLookupTest {
  /* */
  @Test
  public void testPrefixDictionaries() throws IOException {
    final URL url = this.getClass().getResource("test-prefix.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzeczypospolitej"));
    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzecząpospolitą"));

    // This word is not in the dictionary.
    assertNoStemFor(s, "martygalski");
  }

  @Test
  public void testInputConversion() throws IOException {
    final URL url = this.getClass().getResource("test-prefix.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzecz\\apospolit\\a"));

    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "krowa\\apospolit\\a"));
  }

  /* */
  @Test
  public void testInfixDictionaries() throws IOException {
    final URL url = this.getClass().getResource("test-infix.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzeczypospolitej"));
    assertArrayEquals(new String[] { "Rzeczycki", "adj:pl:nom:m" }, stem(s,
        "Rzeczyccy"));
    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzecząpospolitą"));

    // This word is not in the dictionary.
    assertNoStemFor(s, "martygalski");
    assertNoStemFor(s, "Rzeczyckiõh");
  }

  /* */
  @Test
  public void testWordDataIterator() throws IOException {
    final URL url = this.getClass().getResource("test-infix.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

    final HashSet<String> entries = new HashSet<String>();
    for (WordData wd : s) {
      entries.add(wd.getWord() + " " + wd.getStem() + " " + wd.getTag());
    }

    // Make sure a sample of the entries is present.
    assertTrue(entries.contains("Rzekunia Rzekuń subst:sg:gen:m"));
    assertTrue(entries
        .contains("Rzeczkowskie Rzeczkowski adj:sg:nom.acc.voc:n+adj:pl:acc.nom.voc:f.n"));
    assertTrue(entries
        .contains("Rzecząpospolitą Rzeczpospolita subst:irreg"));
    assertTrue(entries
        .contains("Rzeczypospolita Rzeczpospolita subst:irreg"));
    assertTrue(entries
        .contains("Rzeczypospolitych Rzeczpospolita subst:irreg"));
    assertTrue(entries
        .contains("Rzeczyckiej Rzeczycki adj:sg:gen.dat.loc:f"));
  }

  /* */
  @Test
  public void testWordDataCloning() throws IOException {
    final URL url = this.getClass().getResource("test-infix.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

    ArrayList<WordData> words = new ArrayList<WordData>();
    for (WordData wd : s) {
      WordData clone = wd.clone();
      words.add(clone);
    }

    // Reiterate and verify that we have the same entries.
    final DictionaryLookup s2 = new DictionaryLookup(Dictionary.read(url));
    int i = 0;
    for (WordData wd : s2) {
      WordData clone = words.get(i++);
      assertEqualSequences(clone.getStem(), wd.getStem());
      assertEqualSequences(clone.getTag(), wd.getTag());
      assertEqualSequences(clone.getWord(), wd.getWord());
      assertEqualSequences(clone.wordCharSequence, wd.wordCharSequence);
    }

    // Check collections contract.
    final HashSet<WordData> entries = new HashSet<WordData>();
    try {
      entries.add(words.get(0));
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  private void assertEqualSequences(CharSequence s1, CharSequence s2) {
    assertEquals(s1.toString(), s2.toString());
  }

  /* */
  @Test
  public void testMultibyteEncodingUTF8() throws IOException {
    final URL url = this.getClass().getResource("test-diacritics-utf8.dict");
    Dictionary read = Dictionary.read(url);
    final IStemmer s = new DictionaryLookup(read);

    for (byte[] ba : FSAUtils.rightLanguage(read.fsa, read.fsa.getRootNode())) {
      System.out.println(new String(ba, "UTF-8"));
    }

    assertArrayEquals(new String[] { "merge", "001" }, stem(s, "mergeam"));
    assertArrayEquals(new String[] { "merge", "002" }, stem(s, "merseserăm"));
  }

  /* */
  @Test
  public void testSynthesis() throws IOException {
    final URL url = this.getClass().getResource("test-synth.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    assertArrayEquals(new String[] { "miała", null }, stem(s,
        "mieć|verb:praet:sg:ter:f:?perf"));
    assertArrayEquals(new String[] { "a", null }, stem(s, "a|conj"));
    assertArrayEquals(new String[] {}, stem(s, "dziecko|subst:sg:dat:n"));

    // This word is not in the dictionary.
    assertNoStemFor(s, "martygalski");
  }

  /* */
  @Test
  public void testInputWithSeparators() throws IOException {
    final URL url = this.getClass().getResource("test-separators.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

    /*
     * Attemp to reconstruct input sequences using WordData iterator.
     */
    ArrayList<String> sequences = new ArrayList<String>();
    for (WordData wd : s) {
      sequences.add("" + wd.getWord() + " " + wd.getStem() + " "
          + wd.getTag());
    }
    Collections.sort(sequences);

    assertEquals("token1 null null", sequences.get(0));
    assertEquals("token2 null null", sequences.get(1));
    assertEquals("token3 null +", sequences.get(2));
    assertEquals("token4 token2 null", sequences.get(3));
    assertEquals("token5 token2 null", sequences.get(4));
    assertEquals("token6 token2 +", sequences.get(5));
    assertEquals("token7 token2 token3+", sequences.get(6));
    assertEquals("token8 token2 token3++", sequences.get(7));
  }

  /* */
  @Test
  public void testSeparatorInLookupTerm() throws IOException {
    FSA fsa = FSABuilder.build(toBytes("iso8859-1", new String [] {
        "l+A+LW",
        "l+A+NN1d",
    }));

    DictionaryMetadata metadata = new DictionaryMetadataBuilder()
    .separator('+')
    .encoding("iso8859-1")
    .encoder(EncoderType.INFIX)
    .build();

    final DictionaryLookup s = new DictionaryLookup(new Dictionary(fsa, metadata));
    assertEquals(0, s.lookup("l+A").size());
  }

  /* */
  @Test
  public void testGetSeparator() throws IOException {
    final URL url = this.getClass().getResource("test-separators.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
    assertEquals('+', s.getSeparatorChar());
  }

  private static byte[][] toBytes(String charset, String[] strings) {
    byte [][] out = new byte [strings.length][];
    for (int i = 0; i < strings.length; i++) {
      try {
        out[i] = strings[i].getBytes(charset);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return out;
  }

  /* */
  public static String asString(CharSequence s) {
    if (s == null)
      return null;
    return s.toString();
  }

  /* */
  public static String[] stem(IStemmer s, String word) {
    ArrayList<String> result = new ArrayList<String>();
    for (WordData wd : s.lookup(word)) {
      result.add(asString(wd.getStem()));
      result.add(asString(wd.getTag()));
    }
    return result.toArray(new String[result.size()]);
  }

  /* */
  public static void assertNoStemFor(IStemmer s, String word) {
    assertArrayEquals(new String[] {}, stem(s, word));
  }
}
