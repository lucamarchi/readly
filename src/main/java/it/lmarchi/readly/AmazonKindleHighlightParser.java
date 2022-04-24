package it.lmarchi.readly;

import static java.util.stream.Collectors.toList;

import it.lmarchi.readly.model.Highlight;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/** A component that fetches and parses the highlights from an Amazon Kindle device. */
final class AmazonKindleHighlightParser {
  private static final Logger LOG = LoggerFactory.getLogger(AmazonKindleHighlightParser.class);
  /** A regex matching the structure of the Kindle clippings file. */
  private static final Pattern HIGHLIGHT_PATTERN =
      Pattern.compile(
          "^(.*)\s\\((.*)\\)(\\r?\\n|\\r)-.*(\\r?\\n|\\r){2}(.*)(\\r?\\n|\\r)={10}(\\r?\\n|\\r)+",
          Pattern.MULTILINE);

  private final String kindleDeviceBasePath;

  AmazonKindleHighlightParser(@Value("${amazon-kindle.base-path}") String kindleDeviceBasePath) {
    this.kindleDeviceBasePath = kindleDeviceBasePath;
  }

  /** Returns the highlights of the books in the provided Amazon Kindle device. */
  List<Highlight> getHighlights() {
    String clippingFileContent = getClippingFileContent();

    return HIGHLIGHT_PATTERN.matcher(clippingFileContent)
        .results()
        .map(AmazonKindleHighlightParser::toHighlight)
        .peek(highlight ->
            LOG.info("Found the highlight for book '{}' of author '{}'", highlight.title(), highlight.author()))
        .filter(highlight -> !highlight.content().isEmpty())
        .collect(toList());
  }

  /** Returns the content of the file containing the Kindle highlights. */
  private String getClippingFileContent() {
    Path clippingFile = Path.of(kindleDeviceBasePath, "documents", "My clippings.txt");
    try {
      return Files.readString(clippingFile);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read the Kindle clipping file", e);
    }
  }

  /** Returns the highlight of a book from the given regex match. */
  private static Highlight toHighlight(MatchResult matcher) {
    return new Highlight(matcher.group(1), matcher.group(2), matcher.group(5));
  }
}
