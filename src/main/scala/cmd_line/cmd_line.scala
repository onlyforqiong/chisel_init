package examples


import chisel3._
import chisel3.stage._
import chisel3.util._



class Vga extends BlackBox  {
  val io = IO(new Bundle {
    val clk = Input(Bool())
    val rst = Input(Reset())
    val vga_data = Input(UInt(12.W))
    val h_addr = Output(UInt(10.W))
    val v_addr = Output(UInt(10.W))
    val hsync = Output(Bool())
    val vsync = Output(Bool())
    val valid = Output(Bool())
    val vga_r = Output(UInt(4.W))
    val vga_g = Output(UInt(4.W))
    val vga_b = Output(UInt(4.W))
  })
}


class KeyboardBottom extends BlackBox  {
  val io = IO(new Bundle {
    val clk = Input(Bool())
    val rst = Input(Reset())
    val ps2_clk = Input(Bool())
    val ps2_data = Input(Bool())
    val continue_flag = Output(Bool())
    val keyboard_data = Output(UInt(8.W))
    val loosen_flag = Output(Bool())
    val ready = Output(Bool())
  })
}

class char_table extends BlackBox {
  val io = IO(new Bundle {
    val clka = Input(Bool())
    val ena = Input(Bool())
    val addra = Input(UInt(13.W))
    val douta = Output(UInt(9.W))
  })
}
class scancode_table extends BlackBox {
  val io = IO(new Bundle {
    val clka = Input(Bool())
    val ena = Input(Bool())
    val addra = Input(UInt(8.W))
    val douta = Output(UInt(8.W))
  })
}

class vga_mem extends BlackBox {
  val io = IO(new Bundle {
    val clka = Input(Bool())
    val ena = Input(Bool())
    val wea = Input(UInt(1.W))
    val addra = Input(UInt(12.W))
    val dina = Input(UInt(8.W))
    val clkb = Input(Bool())
    val enb = Input(Bool())
    val addrb = Input(UInt(12.W))
    val doutb = Output(UInt(8.W))
  })
}


class CommandLineTest extends Module {
  // val io = IO(new Bundle {
    val ps2_clk = IO(Input(Bool()))
    val ps2_data = IO(Input(Bool()))
    val vga_vs = IO(Output(Bool()))
    val vga_hs = IO(Output(Bool()))
    val vga_r = IO(Output(UInt(4.W)))
    val vga_g = IO(Output(UInt(4.W)))
    val vga_b = IO(Output(UInt(4.W)))
    val led = IO(Output(UInt(1.W)))
  // })
    val clk_con = Module(new clk_converter)
    clk_con.io.clk_in1 := clock.asBool
    withClock(clk_con.io.clk_out1.asClock) {
        // val vga_controler = Module(new vga(data_width))
           val lookupTable = Module(new scancode_table)
  val charTable = Module(new char_table)
  val screenData = Module(new vga_mem)
  // charTable
  val keyBoardDataSync = RegInit(0.U(24.W))
  // VGA module
  val vgaModule = Module(new Vga)

    // Command line processing
  val commandX = RegInit(0.U(8.W))
  val commandY = RegInit(0.U(8.W))
  val tempAsc = RegInit(0.U(8.W))
  val downSet = RegInit(0.U(12.W))

  vgaModule.io.clk := clk_con.io.clk_out1.asBool
  vgaModule.io.rst := reset
  vgaModule.io.vga_data := 0.U
  vga_hs := vgaModule.io.hsync
//   io.vgaOops, it seems my previous response got cut off. Here's the rest of the implementation for the `CommandLineTest` module in Chisel:

// ```scala
  vga_vs := vgaModule.io.vsync
  // vga_blank_n := vgaModule.io.valid
  // vga_sync_n := 1.U // Always sync on negative edge
  vga_r := vgaModule.io.vga_r
  vga_g := vgaModule.io.vga_g
  vga_b := vgaModule.io.vga_b
  // addr_h := vgaModule.io.h_addr
  // addr_v := vgaModule.io.v_addr
  // io.vga_clk := vgaModule.io.


  //连接查找表
  lookupTable.io.clka := clk_con.io.clk_out1.asBool
  lookupTable.io.ena := 1.U
  lookupTable.io.addra := keyBoardDataSync(7, 0)
  val lookup_data = lookupTable.io.douta

  screenData.io.clka := clk_con.io.clk_out1.asBool
  screenData.io.ena := 1.U.asBool
  screenData.io.addra := commandX + commandY * 70.U
  screenData.io.dina := lookup_data
  // (commandX + commandY * 70.U) := lookup_data


  screenData.io.clkb := clk_con.io.clk_out1.asBool
  screenData.io.enb := 1.U.asBool




  
  
  // Keyboard module
  val keyboardModule = Module(new KeyboardBottom)
  keyboardModule.io.clk :=  clk_con.io.clk_out1.asBool
  keyboardModule.io.rst := reset
  keyboardModule.io.ps2_clk := ps2_clk
  keyboardModule.io.ps2_data := ps2_data

  keyBoardDataSync := Cat(keyBoardDataSync(15, 0), keyboardModule.io.keyboard_data)
  val loosenFlag = RegInit(false.B)
  loosenFlag := keyboardModule.io.loosen_flag
  val ready = RegInit(false.B)
  ready := keyboardModule.io.ready




    when (ready) {
      when (loosenFlag || keyBoardDataSync(15, 8) === 0xF0.U) {
        commandX := commandX
        commandY := commandY
        screenData.io.wea := 0.U.asBool
      } .otherwise {
        // switch (keyBoardDataSync(7, 0)) {
          when(keyBoardDataSync(7, 0) ===  0x5A.U) { // Enter key
            commandX := 0.U
            commandY := commandY + 1.U
            screenData.io.wea := 0.U.asBool
          }
          .otherwise { // Other keys, fill in the blanks
            // $display("key data is %h and table answer is %h", keyBoardDataSync(7, 0), lookupTable(keyBoardDataSync(7, 0)))
            // screenData(commandX + commandY * 70.U) := lookup_data
            screenData.io.wea := 1.U.asBool
            when (commandX < 70.U) {
              commandX := commandX + 1.U
            } .otherwise {
              commandX := 0.U
              commandY := commandY + 1.U
            }
        }
      }
    }

    // VGA data processing
    // for (tempV <- 1 until 17) {
    //   val charY = tempV / 16
    //   for (tempH <- 1 until 321) {
    //     print(tempH)
    //     print("\n")
    //     val charX = tempH / 9
    //     downSet := (charX + charY * 70).U
    //     tempAsc := screenData(downSet)
    //     vgaDataArray(Cat(tempH.asUInt(9,0),tempV.asUInt(8,0))) := Mux(charTable(tempAsc << 4 | ((tempV - 1) % 16).U)((tempH - 1)%9), 0xFFFFFF.U, 0.U)
    //   }
    // }


  // Output LED based on commandX
  led := commandX(0)
    } 
  // val rawTableData = Mem(256, UInt(16.W))


  // Debugging output
  // $display("commandX = %h, commandY = %h, tempAsc = %h, downSet = %h", commandX, commandY, tempAsc, downSet)
}

object cmd_test extends App{
    (new ChiselStage).emitVerilog(new CommandLineTest)
}