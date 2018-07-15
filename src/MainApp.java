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
//        size(800, 800, P2D);
        fullScreen(P2D, 1);
    }

    ControlP5 cp5;

    Accordion accordion;

    float velocityMultiplier = 0;
    int spawnCount = 2; //must be > 0

    float fluid_temp_r = 0;
    float fluid_temp_value = 0;
    float fluid_density_r = 5;
    float fluid_density_intensity = 5;
    float fft_clamp = 20;
    float fft_mult = 20;
    float binary_clamp = 7;

    float hueStart = .6f;
    float hueEnd = 1.f;

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
        fft.linAverages( fft.specSize()/spawnCount);
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
                for(int i = 0; i < fft.avgSize(); i+=spawnCount){
                    float px  = map(i, 0,  fft.avgSize(), 0, width);
                    float py = height/12;
                    float distanceFromMiddle = getAbs(width / 2 - px);
                    int fftIndex = round(map(distanceFromMiddle, 0, width/2, 0, fft.avgSize()-2));
                    float fft_val = log( 2000 * fft.getAvg(fftIndex) / fft.timeSize() )*fft_mult;

                    if(fft_val > fft_clamp){
                        fft_val = fft_clamp;
                    }
                    if(fft_val < lowest){
                        lowest = fft_val;
                    }
                    if(fft_val > highest){
                        highest = fft_val;
                    }

                    float vx = 0;
                    float vy = fft_val*velocityMultiplier;

                    float n = map(fft_val, 0, fft_clamp, hueStart, hueEnd);
                    int hsb = color(n,1,1);

                    float r = (hsb >> 16 & 0xFF)/255f;
                    float g = (hsb >> 8 & 0xFF) /255f;
                    float b = (hsb & 0xFF)      /255f;

                    if(fft_val > binary_clamp){
                        fluid.addTemperature(px,py, fft_val*fluid_temp_r, fft_val*fluid_temp_value);
                        fluid.addDensity(px, py, fluid_density_r,r,g,b, fluid_density_intensity);
                        fluid.addVelocity(px, py, 5, vx, vy);
                    }
                }
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

        int mode = round(cp5.get("modeRadio").getValue());


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
        fft_mult = cp5.getController("fft_mult").getValue();
        fft_clamp = cp5.getController("fft_clamp").getValue();
        hueStart = cp5.getController("hue_start").getValue();
        hueEnd = cp5.getController("hue_end").getValue();
    }

    void gui() {

        cp5 = new ControlP5(this);

        // group number 3, contains a bang and a slider
        Group g1 = cp5.addGroup("audio reactive")
                .setBackgroundColor(color(50, 64))
                .setBackgroundHeight(230)
                ;

        Group g2 = cp5.addGroup("general properties")
                .setBackgroundColor(color(50, 64))
                .setBackgroundHeight(125)
                ;

        Group g3 = cp5.addGroup("mode")
                .setBackgroundColor(color(50, 64))
                .setBackgroundHeight(110)
                ;

        Group g4 = cp5.addGroup("hue")
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
                .setValue(13)
                .moveTo(g1)
        ;
        cp5.addBang("reset")
                .setPosition(barW*2, y)
                .setSize( 20, 20)
                .setTriggerEvent(Bang.RELEASE)
                .setLabel("reset")
                .plugTo(this,"reset")
                .moveTo(g1)
        ;
        cp5.addSlider("density_intensity")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,10)
                .setValue(3)
                .moveTo(g1)
        ;
        cp5.addSlider("speed_multiplier")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(-5,5)
                .setValue(2)
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
        cp5.addSlider("fft_mult")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(1,1000)
                .setValue(40)
                .moveTo(g1)
        ;
        cp5.addSlider("fft_clamp")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(1,1000)
                .setValue(1000)
                .moveTo(g1)
        ;
        cp5.addSlider("binary_clamp")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(1,250)
                .setValue(7)
                .moveTo(g1)
        ;

        y = -yOff/2;

        cp5.addSlider("vorticity")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,5)
                .setValue(.99f)
                .moveTo(g2)
        ;
        cp5.addSlider("dissipation_velocity")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,1.2f)
                .setValue(.7f)
                .moveTo(g2)
        ;
        cp5.addSlider("dissipation_density")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,1.2f)
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
        cp5.addRadio("modeRadio")
                .setPosition(0, y+=yOff)
                .setSize(barW, 20)
                .addItem("density", 0)
                .addItem("temperature", 1)
                .addItem("pressure", 2)
                .addItem("velocity", 3)
                .activate(0)
                .moveTo(g3);
        y = -yOff/2;
        cp5.addSlider("hue_start")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,1)
                .setValue(.90f)
                .moveTo(g4)
        ;
        cp5.addSlider("hue_end")
                .setPosition(0,y+=yOff)
                .setSize(barW,20)
                .setRange(0,1)
                .setValue(.58f)
                .moveTo(g4)
        ;
        // create a new accordion
        // add g1 to the accordion.
        accordion = cp5.addAccordion("acc")
                .setPosition(40,40)
                .setWidth(200)
                .addItem(g1)
                .addItem(g2)
                .addItem(g3)
                .addItem(g4)
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
        accordion.open(3);

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