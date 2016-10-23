/*
	bechin
	This class simulates a process table by reading in a file from the command
	line.
*/
import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.FileNotFoundException;

public class ProcessTable{

	private static final int WHEN = 0;
	private static final int HOW_LONG = 1;
	private static final int READY = 2;
	private static List<Process> table = new ArrayList<>();
	private static int totalCycles = 0;
	private static volatile boolean needToUnblock = true;

	public static void main(String[] args) throws FileNotFoundException {
		if(args.length != 1)
			throw new IllegalArgumentException("Requires one filename argument.");
		processFile(args[0]);

		int curProcNum = 0;
		Process curProc = table.get(curProcNum);
		while(totalCycles > 0){
			if(curProc.getStatus() == "ready"){
				int curProcCyc = 0;
				curProc.status = 0; //running
				System.out.println(curProc.id + " " + curProc.getStatus());
				int calcEndPc = Math.min(curProc.cycles, curProc.pc + 100);
				if(curProc.blockingTimes != null && 
				   curProc.block < curProc.blockingTimes.size())
					calcEndPc = Math.min(calcEndPc, curProc.blockingTimes.get(curProc.block)[WHEN]);
				System.out.println("pc " + curProc.pc + " -> " + calcEndPc);
				while(curProc.pc < curProc.cycles && curProcCyc < 100){
					if(curProc.blockingTimes != null && 
					   curProc.block < curProc.blockingTimes.size() &&
					   curProc.pc == curProc.blockingTimes.get(curProc.block)[WHEN]){
						curProc.status = 1; //blocked
						break;
					}
					curProcCyc++;
					curProc.pc++;
					totalCycles--;
					checkBlocked();
				}
				if(curProc.getStatus() != "blocked")
					curProc.status = 2; //ready
				System.out.println(curProc.id + " " + curProc.getStatus());
				if(curProc.pc == curProc.cycles){
					curProc.status = 3; //done
					System.out.println(curProc.id + " " + curProc.getStatus());
				}
			}
			curProcNum = ++curProcNum%table.size();
			curProc = table.get(curProcNum);
		}
	}

	private static void processFile(String filename) throws FileNotFoundException {
		Scanner fileScanner = new Scanner(new File(filename));
		while(fileScanner.hasNext()){
			String[] tokens = fileScanner.nextLine().split(",");
			//assuming pid is the same as table index + 1
			int id = Integer.parseInt(tokens[0]);
			int pc = Integer.parseInt(tokens[1]);
			int cycles = Integer.parseInt(tokens[2]);
			totalCycles += cycles;
			String blockingTimes = tokens[3];
			table.add(new Process(id, pc, READY, cycles, blockingTimes));
		}
	}

	private static void checkBlocked(){
		for(int i = 0; i < table.size(); i++){
			Process p = table.get(i);
			if(p.getStatus() == "blocked"){
				p.blockingTimes.get(p.block)[HOW_LONG]--;
				if(p.blockingTimes.get(p.block)[HOW_LONG] == 0){
					p.block++;
					p.status = 2; //ready
					System.out.println(p.id + " " + p.getStatus());
				}
			}
		}
	}

	private static class Process{

		protected int id;
		protected int pc;
		protected int status;
		protected int cycles;
		protected int block;
		protected List<int[]> blockingTimes;

		public Process(){
		}

		public Process(int id, int pc, int status, int cycles, String blockingTimes){
			this.id = id;
			this.pc = pc;
			this.status = status;
			this.cycles = cycles;
			block = 0;
			if(!blockingTimes.equals("0")){
				this.blockingTimes = new ArrayList<int[]>();
				for(String block : blockingTimes.split(":")){
					String[] startDuration = block.split(";");
					int[] stDur = new int[2];
					for(int i = 0; i < startDuration.length; i++)
						stDur[i] = Integer.parseInt(startDuration[i]);
					this.blockingTimes.add(stDur);
				}
			}
		}

		private String getStatus(){
			switch(status){
				case 0:
					return "running";
				case 1:
					return "blocked";
				case 2:
					return "ready";
				case 3:
					return "done";
			}
			return null;
		}
		
	}

}
