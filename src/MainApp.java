import ddf.minim.AudioInput;
import ddf.minim.Minim;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;

import processing.core.PApplet;

// FLUID SIMULATION EXAMPLE
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;
import processing.opengl.PGraphics2D;


import controlP5.*;

public class MainApp extends PApplet {

    public static void main(String args[]) {
        PApplet.main(new String[] { MainApp.class.getName() });
    }

    public void settings(){
        size(800, 800, P2D);
//        fullScreen(P2D, 1);
    }

    ControlP5 cp5;

    Accordion accordion;

    float velocityMultiplier = 0;
    int spawnCount = 2; //must be > 0

    float fluid_temp_r = 0;
    float fluid_temp_value = 0;
    float fluid_density_r = 5;
    float fluid_density_intensity = 5;

    Minim m;
    AudioInput in;
    FFT fft;
    BeatDetect bd;

    // fluid simulation
    DwFluid2D fluid;

    // render target
    PGraphics2D pg_fluid;

    public void setup() {
        colorMode(HSB, 1.f);
        gui();
        m = new Minim(this);
        in = m.getLineIn();
        fft = new FFT(in.mix.size(), in.sampleRate());
        bd = new BeatDetect();

        // library context
        DwPixelFlow context = new DwPixelFlow(this);

        // fluid simulation
        fluid = new DwFluid2D(context, width, height, 2);


        // adding data to the fluid simulation
        fluid.addCallback_FluiData(new  DwFluid2D.FluidData() {
            public void update(DwFluid2D fluid) {
                float highest = 0;
                float lowest = 0;
                float bandClamp = 20;

                for(int i = 0; i < fft.specSize(); i+=spawnCount){

                    float px  = map(i, 0,  fft.specSize(), 0, width);
                    float py = height/8;
                    float distanceFromMiddle = getAbs(width / 2 - px);
                    int fftIndex = round(map(distanceFromMiddle, 0, width/2, 0, fft.specSize()));
                    float band = fft.getBand(fftIndex)*30;

                    if(band > bandClamp){
                        band = bandClamp;
                    }
                    if(band < lowest){
                        lowest = band;
                    }
                    if(band > highest){
                        highest = band;
                    }


                    float vx = 0;
                    float vy = band*velocityMultiplier;

                    float n = map(band, 0, bandClamp, 0, 1);
                    int hsb = color(n,1,1);

                    float r = (hsb >> 16 & 0xFF) /255;
                    float g = (hsb >> 8 & 0xFF)  /255;
                    float b = (hsb & 0xFF)       /255;

                    if(band > bandClamp/3){
                        fluid.addTemperature(px,py, band*fluid_temp_r, band*fluid_temp_value);
                        fluid.addDensity(px, py, fluid_density_r,r,g,b, fluid_density_intensity);
                        fluid.addVelocity(px, py, 5, vx, vy);
                    }
                }
//                println(lowest+":"+highest);
            }
        });

        pg_fluid = (PGraphics2D) createGraphics(width, height, P2D);


    }

    float getAbs(float in){
        if(in<0)in*=-1;
        return in;
    }

    public void draw() {
        bd.detect(in.mix);
        fft.forward(in.mix);

        // update simulation
        fluid.update();

        // clear render target
        pg_fluid.beginDraw();
        pg_fluid.background(0);
        pg_fluid.endDraw();

        int mode = round(cp5.get("mode").getValue());


        // render fluid stuff
        fluid.renderFluidTextures(pg_fluid,mode);

        // display
        image(pg_fluid, 0, 0, width, height);

        fluid.param.vorticity = cp5.getController("vorticity").getValue();
        fluid.param.dissipation_velocity = cp5.getController("dissipation_velocity").getValue();
        fluid.param.dissipation_density  = cp5.getController("dissipation_density").getValue();
        velocityMultiplier = cp5.getController("speed_multiplier").getValue();
        spawnCount = round(cp5.getController("spawn_count").getValue());
        fluid_density_r = cp5.getController("density_r").getValue();
        fluid_density_intensity = cp5.getController("density_intensity").getValue();
        fluid_temp_r = cp5.getController("fluid_temp_r").getValue();
        fluid_temp_value = cp5.getController("fluid_temp_value").getValue();

    }

    void gui() {

        cp5 = new ControlP5(this);

        // group number 3, contains a bang and a slider
        Group g1 = cp5.addGroup("audio reactive")
                .setBackgroundColor(color(50, 64))
                .setBackgroundHeight(150)
                ;

        Group g2 = cp5.addGroup("general properties")
                .setBackgroundColor(color(50, 64))
                .setBackgroundHeight(150)
                ;

        Group g3 = cp5.addGroup("mode")
                .setBackgroundColor(color(50, 64))
                .setBackgroundHeight(150)
                ;


        float yOff = 25;
        float y = -yOff/2;
        int barW = 100;

        cp5.addSlider("density_r")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(1,20)
                .setValue(4)
                .moveTo(g1)
        ;
        cp5.addSlider("density_intensity")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,3)
                .setValue(1)
                .moveTo(g1)
        ;

        cp5.addSlider("speed_multiplier")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,100)
                .setValue(4)
                .moveTo(g1)
        ;

        cp5.addSlider("fluid_temp_r")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,1)
                .setValue(0)
                .moveTo(g1)
        ;

        cp5.addSlider("fluid_temp_value")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(-1,1)
                .setValue(0)
                .moveTo(g1)
        ;

        y = -yOff/2;

        cp5.addSlider("vorticity")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,20)
                .setValue(.99f)
                .moveTo(g2)
        ;


        cp5.addSlider("dissipation_velocity")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,2)
                .setValue(.7f)
                .moveTo(g2)
        ;

        cp5.addSlider("dissipation_density")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,2)
                .setValue(.8f)
                .moveTo(g2)
        ;

        cp5.addSlider("spawn_count")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(1,20)
                .setValue(4)
                .moveTo(g2)
        ;

        y = -yOff/2;

        cp5.addRadio("mode")
                .setPosition(0, y+=yOff)
                .setSize(barW, 20)
                .addItem("density", 0)
                .addItem("temperature", 1)
                .addItem("pressure", 2)
                .addItem("velocity", 3)
                .activate(0)
                .moveTo(g3);

        cp5.addBang("reset")
                .setPosition(0, y+=yOff+80)
                .setSize( barW, 20)
                .setTriggerEvent(Bang.RELEASE)
                .setLabel("reset")
                .plugTo(this,"reset")
                .moveTo(g3);

        // create a new accordion
        // add g1 to the accordion.
        accordion = cp5.addAccordion("acc")
                .setPosition(40,40)
                .setWidth(200)
                .addItem(g1)
                .addItem(g2)
                .addItem(g3)
        ;

        cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.open(0,1,2);}}, 'o');
        cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.close(0,1,2);}}, 'c');
        cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setWidth(300);}}, '1');
        cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setPosition(0,0);accordion.setItemHeight(280);}}, '2');
        cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setCollapseMode(ControlP5.ALL);}}, '3');
        cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setCollapseMode(ControlP5.SINGLE);}}, '4');
        cp5.mapKeyFor(new ControlKey() {public void keyEvent() {cp5.remove("myGroup1");}}, '0');


        accordion.open(0);
        accordion.open(1);
        accordion.open(2);

        // use Accordion.MULTI to allow multiple group
        // to be open at a time.
        accordion.setCollapseMode(Accordion.MULTI);

        // when in SINGLE mode, only 1 accordion
        // group can be open at a time.
        // accordion.setCollapseMode(Accordion.SINGLE);
    }

    public void reset(){
        fluid.reset();
    }
}