package com.billy.android.pools;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 线程安全的对象池
 *
 * 使用方法：
 * <pre>
 *     //创建一个新对象池
 *     ObjPool&lt;ImageView, Context&gt; pool = new ObjPool&lt;&gt;(){
 *         protected ImageView newInstance(Context r) {
 *             return new ImageView(r);
 *         }
 *     };
 *     pool.get(context); //获取一个实例
 *     pool.put(imageView); //回收一个实例到对象池
 *     pool.clear(); //清空对象池
 * </pre>
 *
 * @param <T> 要创建的对象
 * @param <R> 创建对象所需的参数
 */
public abstract class ObjPool<T, R> {

	protected ConcurrentLinkedQueue<T> list;

	/**
	 * 创建一个对象池，不限制缓存数量
	 */
	public ObjPool() {
		list = new ConcurrentLinkedQueue<>();
	}

	/**
	 * 从对象池中获取一个实例
	 * 优先从缓存中获取，如果缓存中没有实例，则创建一个实例并返回
	 * @param r 创建一个新实例需要的参数
	 * @return 获取的实例
	 */
	public T get(R r) {
		T t = list.poll();
		if (t == null) {
		    t = newInstance(r);
		}
		if (t != null && t instanceof Initable) {
			((Initable<R>) t).init(r);
		}
		return t;
	}


	/**
	 * 接收一个实例放到对象池中
	 * @param t 要放入对象池的实例
	 */
	public void put(T t) {
		if (t != null) {
			if (t instanceof Resetable) {
			    ((Resetable) t).reset();
			}
			list.offer(t);
		}
	}

	/**
	 * 清空对象池
	 * 推荐在确定不再需要此对象池的时候调用此方法来清空缓存的实例
	 */
	public void clear() {
		list.clear();
	}

	/**
	 * 创建新的实例
	 * @param r 创建对象所需的参数
	 * @return 新的实例
	 */
	protected abstract T newInstance(R r);

	public interface Resetable {
		void reset();
	}
	public interface Initable<R> {
		void init(R r);
	}
}
