package com.lurninghut.controller;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.zkoss.zhtml.Text;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Textbox;

import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class BlogController extends SelectorComposer<Component> {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private Component mainBox;
  private RestHighLevelClient restHighLevelClient;
  private Integer paraCount;
  private Integer gistCount;
  private Integer headerCount;
  private Integer subHeaderCount;
  private Integer imageCount;
  @Wire
  private Textbox title;
  private Configuration cfg;

  @Override
  public void doAfterCompose(Component comp) throws Exception {
    super.doAfterCompose(comp);
    this.mainBox = comp.getChildren().get(0);
    this.restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    initFreemarkerConfig();
    this.paraCount = 0;
    this.gistCount = 0;
    this.headerCount = 0;
    this.subHeaderCount = 0;
    this.imageCount = 0;
  }

  private void initFreemarkerConfig() {
    cfg = new Configuration(Configuration.VERSION_2_3_30);
    WebappTemplateLoader templateLoader = new WebappTemplateLoader(
        Sessions.getCurrent().getWebApp().getServletContext(), "WEB-INF/templates");
    cfg.setTemplateLoader(templateLoader);
    cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
  }

  @Listen("onClick = #addParagraph")
  public void addParagraph() {
    Hbox hbox = new Hbox();
    Textbox textbox = new Textbox();
    textbox.setId("para-" + ++this.paraCount);
    textbox.setRows(10);
    textbox.setAttribute("multiline", true);
    textbox.setCols(100);
    textbox.enableBindingAnnotation();
    hbox.getChildren().add(textbox);
    addDeleteButton(hbox);
    addToMain(hbox);
  }

  private void addToMain(Component component) {
    Separator separator = new Separator();
    separator.setHeight("20px");
    mainBox.getChildren().add(separator);
    mainBox.getChildren().add(component);
  }

  @Listen("onClick = #addGist")
  public void addGist() {
    addInput("Gist Script : ", "gist-" + ++this.gistCount);
  }

  @Listen("onClick = #addHeading")
  public void addHeading() {
    addInput("Add Heading", "header-" + ++this.headerCount);
  }

  @Listen("onClick = #addSubHeading")
  public void addSubHeading() {
    addInput("Add Sub Heading", "subheader-" + ++this.subHeaderCount);
  }

  @Listen("onClick = #uploadImage")
  public void addImage() {
    System.out.println("called upload");
    Hbox hbox = new Hbox();
    Button button = new Button();
    button.setId("image-" + ++this.imageCount);

    button.addEventListener("onUpload", new EventListener<Event>() {
      @Override
      public void onEvent(Event event) throws Exception {
        UploadEvent uploadEvent = (UploadEvent) event;
        org.zkoss.util.media.Media media = uploadEvent.getMedia();
        if (media instanceof org.zkoss.image.Image) {
          org.zkoss.image.Image meidaImg = (org.zkoss.image.Image) media;
          event.getTarget().setAttribute("base64Img", convertImageIconToBase64String(meidaImg.toImageIcon()));
          org.zkoss.zul.Image image = new org.zkoss.zul.Image();
          image.setContent((org.zkoss.image.Image) media);
          image.setParent(hbox);
        } else {
          Messagebox.show("Not an image: " + media, "Error", Messagebox.OK, Messagebox.ERROR);
        }
      }
    });
    button.setLabel("Upload Image");
    button.setUpload("true,maxsize=300");
    hbox.getChildren().add(button);
    addDeleteButton(hbox);
    addToMain(hbox);
  }

  private void addInput(String inputText, String id) {
    Text text = new Text(inputText);
    Hbox hbox = new Hbox();
    hbox.getChildren().add(text);
    Textbox textbox = new Textbox();
    textbox.setId(id);
    textbox.setWidth("400px");
    textbox.enableBindingAnnotation();
    hbox.getChildren().add(textbox);
    addDeleteButton(hbox);
    addToMain(hbox);
  }

  public void addDeleteButton(Component component) {
    Button button = new Button();
    button.setLabel("Delete");
    button.addEventListener("onClick", new EventListener<Event>() {
      @Override
      public void onEvent(Event event) throws Exception {
        event.getTarget().getParent().getChildren().clear();
      }
    });
    component.getChildren().add(button);
  }

  @Listen("onClick = #saveEntry")
  public void saveEntry() {
    Map<String, String> dataMap = createDataMap();
    createHTML(dataMap);
    //indexEntry(dataMap);
  }

  private void createHTML(Map<String, String> dataMap) {
    try {
      Template template = cfg.getTemplate("index.ftl");
      Writer consoleWriter = new OutputStreamWriter(System.out);
      Map<String, Object> templateData = new HashMap<>();
      templateData.put("row", dataMap);
      template.process(templateData, consoleWriter);
    } catch (IOException | TemplateException e) {
      e.printStackTrace();
    }
    
  }

  private Map<String, String> createDataMap() {
    Map<String, String> dataMap = new LinkedHashMap<>();
    dataMap.put("title", this.title.getValue());
    for (Component component : mainBox.getChildren()) {
      if (component instanceof Hbox) {
        for (Component component1 : component.getChildren()) {
          if (component1.getId().contains("para") || component1.getId().contains("header") || component1.getId().contains("image")
              || component1.getId().contains("gist")) {
            if (component1 instanceof Textbox) {
              dataMap.put(component1.getId(), ((Textbox) component1).getValue());
            }
            else if (component1 instanceof Button) {
                dataMap.put(component1.getId(), (String) ((Button) component1).getAttribute("base64Img"));
            }
          }
        }
      }
    }
    return dataMap;
  }

  private void indexEntry(Map<String, String> dataMap) {
    try {
      IndexRequest indexRequest = new IndexRequest("posts");
      indexRequest.source(dataMap, XContentType.JSON);
      this.restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }
    catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String convertImageIconToBase64String(ImageIcon ii) {
    // Create a buffered image of the size of the original image icon
    BufferedImage image = new BufferedImage(ii.getIconWidth(),
        ii.getIconHeight(), BufferedImage.TYPE_INT_RGB);

    // Create a graphics object to draw the image
    Graphics g = image.createGraphics();

    // Paint the icon on to the buffered image
    ii.paintIcon(null, g, 0, 0);
    g.dispose();

    // Convert the buffered image into a byte array
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "jpg", b);
    } catch (Exception ex) {
      // Handle the exception
    }
    byte[] imageInByte = b.toByteArray();

    // Return the Base64 encoded String
    return new String(Base64.getEncoder().encode(imageInByte));
  }
}
