

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FPTree {

	private int minSup; 

	public int getMinSup() {
		return minSup;
	}

	public void setMinSup(int minSup) {
		this.minSup = minSup;
	}

	/**
	 * 1.initialized data
	 * 
	 * @param filenames
	 * @return
	 */
	public List<List<String>> initData() {
		List<List<String>> records = new LinkedList<List<String>>();
		List<String> record;
		try {
			InputStream is = this.getClass().getResourceAsStream("/resource/zoo.data");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.trim() != "") {
					record = new LinkedList<String>();
					String[] items = line.split(",");
					for (String item : items) {
						record.add(item);
					}
					records.add(record);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return records;
	}

	/**
	 * 2. Construction frequent itemsets
	 * 
	 * @param transRecords
	 * @return
	 */
	public ArrayList<TreeNode> buildF1Items(List<List<String>> transRecords) {
		ArrayList<TreeNode> F1 = null;
		if (transRecords.size() > 0) {
			F1 = new ArrayList<TreeNode>();
			Map<String, TreeNode> map = new HashMap<String, TreeNode>();
			// cal the support count of data records
			for (List<String> record : transRecords) {
				for (String item : record) {
					if (!map.keySet().contains(item)) {
						TreeNode node = new TreeNode(item);
						node.setCount(1);
						map.put(item, node);
					} else {
						map.get(item).countIncrement(1);
					}
				}
			}
			Set<String> names = map.keySet();
			for (String name : names) {
				TreeNode tnode = map.get(name);
				if (tnode.getCount() >= minSup) {
					F1.add(tnode);
				}
			}
			Collections.sort(F1);
			return F1;
		} else {
			return null;
		}
	}

	/**
	 * 3.bulid FP-Tree
	 * 
	 * @param transRecords
	 * @param F1
	 * @return
	 */
	public TreeNode buildFPTree(List<List<String>> transRecords, ArrayList<TreeNode> F1) {
		TreeNode root = new TreeNode(); // set the root
		for (List<String> transRecord : transRecords) {
			LinkedList<String> record = sortByF1(transRecord, F1);
			TreeNode subTreeRoot = root;
			TreeNode tmpRoot = null;
			if (root.getChildren() != null) {
				while (!record.isEmpty() && (tmpRoot = subTreeRoot.findChild(record.peek())) != null) {
					tmpRoot.countIncrement(1);
					subTreeRoot = tmpRoot;// traverse
					record.poll();
				}
			}
			addNodes(subTreeRoot, record, F1);
		}
		return root;
	}

	/**
	 * 3.1 sorting the records according to order of the Frequent itemsets
	 * 
	 * @param transRecord
	 * @param F1
	 * @return
	 */
	public LinkedList<String> sortByF1(List<String> transRecord, ArrayList<TreeNode> F1) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (String item : transRecord) {
			for (int i = 0; i < F1.size(); i++) {
				TreeNode tnode = F1.get(i);
				if (tnode.getName().equals(item)) {
					map.put(item, i);
				}
			}
		}
		ArrayList<Entry<String, Integer>> al = new ArrayList<Entry<String, Integer>>(map.entrySet());
		Collections.sort(al, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> arg0, Entry<String, Integer> arg1) {
				return arg0.getValue() - arg1.getValue();
			}
		});
		LinkedList<String> rest = new LinkedList<String>();
		for (Entry<String, Integer> entry : al) {
			rest.add(entry.getKey());
		}
		return rest;
	}

	/**
	 * 3.2 insert node to the FP-Tree
	 * 
	 * @param ancestor
	 * @param record
	 * @param F1
	 */
	public void addNodes(TreeNode ancestor, LinkedList<String> record, ArrayList<TreeNode> F1) {
		if (record.size() > 0) {
			while (record.size() > 0) {
				String item = record.poll();
				TreeNode leafnode = new TreeNode(item);
				leafnode.setCount(1);
				leafnode.setParent(ancestor);
				ancestor.addChild(leafnode);

				for (TreeNode f1 : F1) {
					if (f1.getName().equals(item)) {
						while (f1.getNextHomonym() != null) {
							f1 = f1.getNextHomonym();
						}
						f1.setNextHomonym(leafnode);
						break;
					}
				}

				addNodes(leafnode, record, F1);
			}
		}
	}

	/**
	 * 4. Find the frequent pattern from the FP-Tree
	 * 
	 * @param root
	 * @param F1
	 * @return
	 */
	public Map<List<String>, Integer> findFP(TreeNode root, ArrayList<TreeNode> F1) {
		Map<List<String>, Integer> fp = new HashMap<List<String>, Integer>();

		Iterator<TreeNode> iter = F1.iterator();
		while (iter.hasNext()) {
			TreeNode curr = iter.next();
			List<List<String>> transRecords = new LinkedList<List<String>>();
			TreeNode backnode = curr.getNextHomonym();
			while (backnode != null) {
				int counter = backnode.getCount();
				List<String> prenodes = new ArrayList<String>();
				TreeNode parent = backnode;
				while ((parent = parent.getParent()).getName() != null) {
					prenodes.add(parent.getName());
				}
				while (counter-- > 0) {
					transRecords.add(prenodes);
				}
				backnode = backnode.getNextHomonym();
			}

			ArrayList<TreeNode> subF1 = buildF1Items(transRecords);
			TreeNode subRoot = buildFPTree(transRecords, subF1);

			if (subRoot != null) {
				Map<List<String>, Integer> prePatterns = findPrePattern(subRoot);
				if (prePatterns != null) {
					Set<Entry<List<String>, Integer>> ss = prePatterns.entrySet();
					for (Entry<List<String>, Integer> entry : ss) {
						entry.getKey().add(curr.getName());
						fp.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}

		return fp;
	}

	/**
	 * 4.1 Find prefix pattern from a FP-Tree
	 * 
	 * @param root
	 * @return
	 */
	public Map<List<String>, Integer> findPrePattern(TreeNode root) {
		Map<List<String>, Integer> patterns = null;
		List<TreeNode> children = root.getChildren();
		if (children != null) {
			patterns = new HashMap<List<String>, Integer>();
			for (TreeNode child : children) {
				LinkedList<LinkedList<TreeNode>> paths = buildPaths(child);
				if (paths != null) {
					for (List<TreeNode> path : paths) {
						Map<List<String>, Integer> backPatterns = combination(path);
						Set<Entry<List<String>, Integer>> entryset = backPatterns.entrySet();
						for (Entry<List<String>, Integer> entry : entryset) {
							List<String> key = entry.getKey();
							int c1 = entry.getValue();
							int c0 = 0;
							if (patterns.containsKey(key)) {
								c0 = patterns.get(key).byteValue();
							}
							patterns.put(key, c0 + c1);
						}
					}
				}
			}
		}

		Map<List<String>, Integer> rect = null;
		if (patterns != null) {
			rect = new HashMap<List<String>, Integer>();
			Set<Entry<List<String>, Integer>> ss = patterns.entrySet();
			for (Entry<List<String>, Integer> entry : ss) {
				if (entry.getValue() >= minSup) {
					rect.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return rect;
	}

	/**
	 * 4.1.1 set a path from root to every leaves
	 * 
	 * @param stack
	 * @param root
	 */
	public LinkedList<LinkedList<TreeNode>> buildPaths(TreeNode root) {
		LinkedList<LinkedList<TreeNode>> paths = null;
		if (root != null) {
			paths = new LinkedList<LinkedList<TreeNode>>();
			List<TreeNode> children = root.getChildren();
			if (children != null) {
				if (children.size() > 1) {
					for (TreeNode child : children) {
						int count = child.getCount();
						LinkedList<LinkedList<TreeNode>> ll = buildPaths(child);
						for (LinkedList<TreeNode> lp : ll) {
							TreeNode prenode = new TreeNode(root.getName());
							prenode.setCount(count);
							lp.addFirst(prenode);
							paths.add(lp);
						}
					}
				}
				else {
					for (TreeNode child : children) {
						LinkedList<LinkedList<TreeNode>> ll = buildPaths(child);
						for (LinkedList<TreeNode> lp : ll) {
							lp.addFirst(root);
							paths.add(lp);
						}
					}
				}
			} else {
				LinkedList<TreeNode> lp = new LinkedList<TreeNode>();
				lp.add(root);
				paths.add(lp);
			}
		}
		return paths;
	}

	/**
	 * 4.1.2
	 * generate the group in the path and record the count
	 * 
	 * @param path
	 * @return
	 */
	public Map<List<String>, Integer> combination(List<TreeNode> path) {
		if (path.size() > 0) {
			TreeNode start = path.remove(0);
			Map<List<String>, Integer> rect = new HashMap<List<String>, Integer>();
			List<String> li = new ArrayList<String>();
			li.add(start.getName());
			rect.put(li, start.getCount());

			Map<List<String>, Integer> postCombination = combination(path);
			if (postCombination != null) {
				Set<Entry<List<String>, Integer>> set = postCombination.entrySet();
				for (Entry<List<String>, Integer> entry : set) {
					rect.put(entry.getKey(), entry.getValue());
					List<String> ll = new ArrayList<String>();
					ll.addAll(entry.getKey());
					ll.add(start.getName());
					rect.put(ll, entry.getValue());
				}
			}

			return rect;
		} else {
			return null;
		}
	}

	/**
	 * output
	 * 
	 * @param F1
	 */
	public void printF1(List<TreeNode> F1) {
		System.out.println("F-1 set: ");
		for (TreeNode item : F1) {
			System.out.print(item.getName() + ":" + item.getCount() + "\t");
		}
		System.out.println();
		System.out.println();
	}

	/**
	 * print FP-Tree
	 * 
	 * @param root
	 */
	public void printFPTree(TreeNode root) {
		printNode(root);
		List<TreeNode> children = root.getChildren();
		if (children != null && children.size() > 0) {
			for (TreeNode child : children) {
				printFPTree(child);
			}
		}
	}

	/**
	 * print information of every node of FP-Tree
	 * 
	 * @param node
	 */
	public void printNode(TreeNode node) {
		if (node.getName() != null) {
			System.out.print(
					"Name:" + node.getName() + "\tCount:" + node.getCount() + "\tParent:" + node.getParent().getName());
			if (node.getNextHomonym() != null)
				System.out.print("\tNextHomonym:" + node.getNextHomonym().getName());
			System.out.print("\tChildren:");
			node.printChildrenName();
			System.out.println();
		} else {
			System.out.println("FPTreeRoot");
		}
	}

	/**
	 * Print the FP-Growth
	 * 
	 * @param patterns
	 * @param transFile
	 * @param f1
	 * @throws IOException
	 */
	public void printFreqPatterns(Map<List<String>, Integer> patterns, ArrayList<TreeNode> f1) throws IOException {
		System.out.println();
		System.out.println("MinSupport=" + this.getMinSup());
		StringBuffer sbf = new StringBuffer("");
		int total = patterns.size() + f1.size();
		sbf.append("Total number of Frequent Patterns is :" + total + "\n");
		sbf.append("Frequent Patterns and their Support\n");
		for (TreeNode tn : f1) {
			sbf.append(tn.getName() + ":" + tn.getCount() + "\n");
		}
		Set<Entry<List<String>, Integer>> ss = patterns.entrySet();
		for (Entry<List<String>, Integer> entry : ss) {
			List<String> list = entry.getKey();
			for (String item : list) {
				sbf.append(item + " ");
			}
			sbf.append("：" + entry.getValue() + "\n");
		}
		System.out.println(sbf);
	}

	public static void main(String[] args) throws IOException {
		FPTree fptree = new FPTree();
		// fptree.setMinSup(3);
		List<List<String>> transRecords = fptree.initData(); 
		fptree.setMinSup((int) (transRecords.size() * 0.25));
		long startTime = System.currentTimeMillis();
		ArrayList<TreeNode> F1 = fptree.buildF1Items(transRecords);
		fptree.printF1(F1);
		TreeNode treeroot = fptree.buildFPTree(transRecords, F1);
		fptree.printFPTree(treeroot);
		Map<List<String>, Integer> patterns = fptree.findFP(treeroot, F1);
		System.out.println("size of F1 = " + F1.size());
		long endTime = System.currentTimeMillis();
		System.out.println("Run-Time: " + (endTime - startTime) + "ms");
		fptree.printFreqPatterns(patterns, F1);
	}
}
