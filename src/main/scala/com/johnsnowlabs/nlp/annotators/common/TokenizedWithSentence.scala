package com.johnsnowlabs.nlp.annotators.common

import com.johnsnowlabs.nlp.{Annotation, AnnotatorType}


object TokenizedWithSentence extends Annotated[TokenizedSentence] {

  override def annotatorType: String = AnnotatorType.TOKEN

  override def unpack(annotations: Seq[Annotation]): Seq[TokenizedSentence] = {
    val tokens = annotations
      .filter(_.annotatorType == annotatorType)
      .toArray

    val hasMeta = annotations.find(_.metadata.get("sentence").nonEmpty).nonEmpty
    val sentences = SentenceSplit.unpack(annotations)

    /** // Evaluate whether to enable this validation to check proper usage of DOCUMENT and SENTENCE within entire pipelines
      * require(tokens.map(_.metadata.getOrElse("sentence", "0").toInt).distinct.length == sentences.length,
      * "Inconsistencies found in pipeline. Tokens in sentences does not match with sentence count")
      */
    val retAnns = if (!hasMeta) {
      sentences.map(sentence => filterTokensFromSentence(sentence, tokens))
        .zipWithIndex.map { case (indexedTokens, index) => TokenizedSentence(indexedTokens.toArray, index) }.filter(_.indexedTokens.nonEmpty)
    } else {
      sentences.flatMap(sentence => filterTokensFromSentence(sentence, tokens))
        .groupBy(_.sentenceIndex).map { case (sentenceIndex, indexedTokens) => TokenizedSentence(indexedTokens.toArray, sentenceIndex) }
        .filter(_.indexedTokens.nonEmpty).toSeq.sortBy(_.sentenceIndex)
    }

    retAnns
  }

  def filterTokensFromSentence(sentence:Sentence, tokens: Seq[Annotation]): Seq[IndexedToken] = {
    val sentenceTokens = tokens.filter(token =>
      token.begin >= sentence.start & token.end <= sentence.end
    ).map(token => IndexedToken(token.result, token.begin, token.end, sentence.index))
    sentenceTokens
  }

  override def pack(sentences: Seq[TokenizedSentence]): Seq[Annotation] = {
    sentences.flatMap{ sentence =>
      sentence.indexedTokens.map{token =>
        Annotation(annotatorType, token.begin, token.end, token.token,
          Map("sentence" -> sentence.sentenceIndex.toString))
      }}
  }
}
