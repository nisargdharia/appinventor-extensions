// -*- mode: javascript; js-indent-level: 2; -*-
// Copyright © 2019 MIT, All rights reserved.

"use strict";

console.log("QuestionAnswerExtension using tfjs-core version " + tf.version_core);
console.log("QuestionAnswerExtension using tfjs-converter version " + tf.version_converter);

const ERROR_WEBVIEW_NO_MEDIA = 400;
const ERROR_MODEL_LOAD = 401;
const videoWidth = 300;
const videoHeight = 250;
const defaultQuantBytes = 2;
const defaultMobileNetMultiplier = 0.50;
const defaultMobileNetStride = 16;
const defaultMobileNetInputResolution = 257;

const ERRORS = {
  400: "WebView does not support navigator.mediaDevices",
  401: "Unable to load model"
};

let net = null;

// async function loadScripts()
// {
//     // Adding the script tag to the head as suggested before
//     var root = document.root;
//     console.log(root.innerHTML);

//     var head = document.head;
//     console.log(head.innerHTML);

//     const template = await loadHTML('./template.html', import.meta.url);
//     document.getElementById('root').innerHTML = template;

//     // var script1 = document.createElement('script');
//     // script1.src = "https://cdn.jsdelivr.net/npm/@tensorflow/tfjs";
//     // head.appendChild(script1);
//     // console.log("QuestionAnswerModel script 1 loaded");

//     // var script2 = document.createElement('script');
//     // script2.src = "https://cdn.jsdelivr.net/npm/@tensorflow-models/qna";

//     // // Then bind the event to the callback function.
//     // // There are several events for cross browser compatibility.
//     // script2.onreadystatechange = loadModel;
//     // script2.onload = loadModel;

//     // // Fire the loading
//     // head.appendChild(script2);
//     console.log("QuestionAnswerModel script 2 loaded");
// }

async function loadModel() {
  console.log("QuestionAnswerExtension calling loadModel()");
  try {
    // Adding the script tag to the head as suggested before
    var head = document.head;
    console.log("QuestionAnswerModel: ", head.innerHTML);
    
    // var qna_script = document.getElementsById("qna_script");
    // console.log("QuestionAnswerModel script vals:", qna_script.getAttribute("question"));
    // console.log("QuestionAnswerModel script vals:", qna_script.getAttribute("passage"));
    // console.log("QuestionAnswerModel script vals:", qna_script.getAttribute("answer"));

    // qna.load().then(model => {
    //   // Find the answers
    //   model.findAnswers(question, passage).then(answers => {
    //     console.log('QuestionAnswerModel Answers:', answers);
    //   });
    // });

    // var script1 = document.createElement('script');
    // script1.src = "https://cdn.jsdelivr.net/npm/@tensorflow/tfjs";
    // head.appendChild(script1);
    // console.log("QuestionAnswerModel script 1 loaded");

    // var script2 = document.createElement('script');
    // script2.src = "https://cdn.jsdelivr.net/npm/@tensorflow-models/qna";
    // head.appendChild(script2);
    // console.log("QuestionAnswerModel script 2 loaded");
    console.log("QuestionAnswerExtension: ", qna);

    const passage = "Google LLC is an American multinational technology company that specializes in Internet-related services and products, which include online advertising technologies, search engine, cloud computing, software, and hardware. It is considered one of the Big Four technology companies, alongside Amazon, Apple, and Facebook. Google was founded in September 1998 by Larry Page and Sergey Brin while they were Ph.D. students at Stanford University in California. Together they own about 14 percent of its shares and control 56 percent of the stockholder voting power through supervoting stock. They incorporated Google as a California privately held company on September 4, 1998, in California. Google was then reincorporated in Delaware on October 22, 2002. An initial public offering (IPO) took place on August 19, 2004, and Google moved to its headquarters in Mountain View, California, nicknamed the Googleplex. In August 2015, Google announced plans to reorganize its various interests as a conglomerate called Alphabet Inc. Google is Alphabet's leading subsidiary and will continue to be the umbrella company for Alphabet's Internet interests. Sundar Pichai was appointed CEO of Google, replacing Larry Page who became the CEO of Alphabet."
    const question = "Who is the CEO of Google?"

    // qna.load().then(model => {
    //   // Find the answers
    //   model.findAnswers(question, passage).then(answers => {
    //     console.log('QuestionAnswerModel Answers: ', answers);
    //   });
    //   return model;
    // });

    // console.log("QuestionAnswerModel loadModel done");
    const model = await qna.load();
    console.log("QuestionAnswerExtension Model:", model);
    const answers = await model.findAnswers(question, passage);
    console.log(answers);
    console.log(typeof(answers));
    console.log("QuestionAnswerExtension BasicTest:", answers);

    return model;
  } catch (e) {
    QuestionAnswerExtension.error(ERROR_MODEL_LOAD,
      ERRORS[ERROR_MODEL_LOAD]);
    throw e;
  }
}

async function useModel() {
  console.log("QuestionAnswerExtension calling useModel()");
  const context = "Google LLC is an American multinational technology company that specializes in Internet-related services and products, which include online advertising technologies," + 
   "search engine, cloud computing, software, and hardware. It is considered one of the Big Four technology companies, alongside Amazon, Apple, and Facebook. Google was founded in" + 
   "September 1998 by Larry Page and Sergey Brin while they were Ph.D. students at Stanford University in California. Together they own about 14 percent of its shares and control 56" +  
   "percent of the stockholder voting power through supervoting stock. They incorporated Google as a California privately held company on September 4, 1998, in California. Google was" + 
   "then reincorporated in Delaware on October 22, 2002. An initial public offering (IPO) took place on August 19, 2004, and Google moved to its headquarters in Mountain View," + 
   "California, nicknamed the Googleplex. In August 2015, Google announced plans to reorganize its various interests as a conglomerate called Alphabet Inc. Google is Alphabet's leading" + 
   "subsidiary and will continue to be the umbrella company for Alphabet's Internet interests. Sundar Pichai was appointed CEO of Google, replacing Larry Page who became the CEO of" + 
   "Alphabet.";
  const question = "Who is the CEO of Google?";

  // console.log("QuestionAnswerExtensionContext: " + context);
  // console.log("QuestionAnswerExtension question: " + question);

  const answers = net.findAnswers(question, context);
  console.log("QuestionAnswerExtension result: " + answers);

  await net.findAnswers(question, context)
  .then(result => {
    console.log("QuestionAnswerExtension result after waiting: " + result);
  })
  .catch(error => {
    console.log("QuestionAnswerExtension error after waiting: " + error);
  })

  // await net.findAnswers(null, context)
  // .then(result => {
  //   console.log("QuestionAnswerExtension result2 after waiting: " + result);
  // })
  // .catch(error => {
  //   console.log("QuestionAnswerExtension error2 after waiting: " + error);
  // })

  console.log("QuestionAnswerExtension type of function: " + net.findAnswers);

  QuestionAnswerExtension.reportResult(answers);
}

loadModel().then(model => {
  console.log("QuestionAnswerExtension Model loaded");
  net = model;
  QuestionAnswerExtension.ready();
});